package com.example.text_a_pic

import android.app.Application
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.android.mms.dom.smil.parser.SmilXmlSerializer
import com.google.android.mms.ContentType
import com.google.android.mms.InvalidHeaderValueException
import com.google.android.mms.MMSPart
import com.google.android.mms.pdu_alt.*
import com.google.android.mms.smil.SmilHelper
import com.klinker.android.send_message.BroadcastUtils
import com.klinker.android.send_message.Transaction
import com.klinker.android.send_message.Transaction.Companion.DEFAULT_EXPIRY_TIME
import com.klinker.android.send_message.Transaction.Companion.DEFAULT_PRIORITY
import com.klinker.android.send_message.Utils
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*
import java.util.*
import kotlin.math.abs

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)

    val contacts = repository.selectAll()
    val selectedContact = repository.recipientId.map { id ->
        id?.let { Resource.success(repository.findById(it)) } ?: Resource.success(null)
    }.flowOn(Dispatchers.IO).asLiveData()

    fun resolveContact(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contact = repository.resolveContact(uri)
            repository.upsert(contact)
            repository.setRecipientId(contact.id)
        }
    }

    fun selectContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setRecipientId(contact.id)
        }
    }

    fun sendPhoto(context: Context, contact: Contact, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val maxSize = smsManager.carrierConfigValues.getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE)
            Timber.v("Max message size: $maxSize")
            val compressed = Compressor.compress(context, file) {
                size(maxSize - 1024L) // Leave some room for headers
            }
            val bytes =
                BufferedInputStream(FileInputStream(compressed)).use { it.buffered().readBytes() }
            Timber.v("Compressed image size: ${bytes.size}")
            val parts = listOf(MMSPart("image", ContentType.IMAGE_JPEG, bytes))
            val addresses = listOf(PhoneNumberUtils.stripSeparators(contact.phoneNumber))
            val fileName = "send.${abs(Random().nextLong())}.dat"
            val sendFile = File(context.cacheDir, fileName)
            val sendReq = buildPdu(context, addresses, null, parts)
            val sentIntent = Intent(Transaction.MMS_SENT)
            BroadcastUtils.addClassName(context, sentIntent, Transaction.MMS_SENT)
            sentIntent.putExtra(Transaction.EXTRA_FILE_PATH, sendFile.path)
            val sentPI = PendingIntent.getBroadcast(
                context,
                0,
                sentIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            FileOutputStream(sendFile).use { writer ->
                writer.write(PduComposer(context, sendReq).make())
                val pduUri = Uri.Builder()
                    .authority(context.packageName + ".MmsFileProvider")
                    .path(fileName)
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .build()
                smsManager.sendMultimediaMessage(context, pduUri, null, null, sentPI)
            }
        }
    }

    private fun buildPdu(
        context: Context,
        recipients: List<String>,
        subject: String?,
        parts: List<MMSPart>
    ): SendReq {
        val req = SendReq()

        Utils.getMyPhoneNumber(context)?.takeIf(String::isNotEmpty)?.let(::EncodedStringValue)
            ?.let(req::setFrom) // From
        recipients.map(::EncodedStringValue).forEach(req::addTo) // To
        subject?.takeIf(String::isNotEmpty)?.let(::EncodedStringValue)
            ?.let(req::setSubject) // Subject

        req.date = System.currentTimeMillis() / 1000
        req.body = PduBody()

        // Parts
        parts.map(this::partToPduPart).forEach { req.body.addPart(it) }

        // SMIL document for compatibility
        req.body.addPart(0, PduPart().apply {
            contentId = "smil".toByteArray()
            contentLocation = "smil.xml".toByteArray()
            contentType = ContentType.APP_SMIL.toByteArray()
            data = ByteArrayOutputStream()
                .apply {
                    SmilXmlSerializer.serialize(
                        SmilHelper.createSmilDocument(req.body),
                        this
                    )
                }
                .toByteArray()
        })

        req.messageSize = parts.mapNotNull { it.data?.size }.sum().toLong()
        req.messageClass = PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray()
        req.expiry = DEFAULT_EXPIRY_TIME

        try {
            req.priority = DEFAULT_PRIORITY
            req.deliveryReport = PduHeaders.VALUE_NO
            req.readReport = PduHeaders.VALUE_NO
        } catch (e: InvalidHeaderValueException) {
            Timber.w(e)
        }

        return req
    }

    private fun partToPduPart(part: MMSPart): PduPart = PduPart().apply {
        val filename = part.name

        // Set Charset if it's a text media.
        if (part.mimeType.startsWith("text")) {
            charset = CharacterSets.UTF_8
        }

        // Set Content-Type.
        contentType = part.mimeType.toByteArray()

        // Set Content-Location.
        contentLocation = filename.toByteArray()
        val index = filename.lastIndexOf(".")
        contentId = (if (index == -1) filename else filename.substring(0, index)).toByteArray()
        data = part.data
    }
}