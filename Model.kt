package app.videogir

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.PrivateContentException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.IOException
import org.schabi.newpipe.extractor.MediaFormat as NpFormat

class UserError(message: String) : Exception(message)

enum class Kind { PROGRESSIVE, MERGE, AUDIO }

data class Option(
    val label: String,
    val detail: String,
    val size: Long,          // بایت؛ 0 یعنی نامشخص
    val kind: Kind,
    val videoUrl: String?,
    val audioUrl: String?
)

data class VideoMeta(
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val thumb: String?
)

data class Loaded(val meta: VideoMeta, val options: List<Option>)

object Extractor {
    private const val CACHE_MS = 10 * 60 * 1000L
    private const val BOT_MSG =
        "یوتیوب IP فعلی (فیلترشکن) را مشکوک دانسته و تأیید «ربات نیستم» خواسته. " +
            "سرور فیلترشکن را عوض کن (ترجیحاً یک کشور دیگر)، چند دقیقه صبر کن و دوباره امتحان کن. " +
            "تکرار پشت‌سرهم وضع را بدتر می‌کند."

    private var cacheUrl: String? = null
    private var cacheAt = 0L
    private var cacheValue: Loaded? = null

    private val urlRegex = Regex("https?://\\S+")
    private val bareYoutube = Regex("(?:www\\.|m\\.|music\\.)?(?:youtube\\.com|youtu\\.be)/\\S+", RegexOption.IGNORE_CASE)

    /** لینک را از متن بیرون می‌کشد؛ لینک بدون http (مثل youtu.be/xxxx) را هم قبول می‌کند. */
    fun findUrl(text: String): String? {
        val full = urlRegex.find(text)?.value
        if (full != null) return full.trimEnd('.', ',', ')', '،', '؛')
        val bare = bareYoutube.find(text)?.value ?: return null
        return "https://" + bare.trimEnd('.', ',', ')', '،', '؛')
    }

    fun load(input: String): Loaded {
        val url = findUrl(input) ?: throw UserError("لینک معتبری پیدا نشد.")

        // همان لینک را تا ۱۰ دقیقه دوباره از یوتیوب نمی‌پرسیم (کمتر درخواست = کمتر خطر بلاک)
        synchronized(this) {
            val c = cacheValue
            if (c != null && url == cacheUrl && System.currentTimeMillis() - cacheAt < CACHE_MS) return c
        }

        val service = try {
            NewPipe.getServiceByUrl(url)
        } catch (e: ExtractionException) {
            throw UserError("این لینک پشتیبانی نمی‌شود.")
        }
        if (service.serviceId != ServiceList.YouTube.serviceId) {
            throw UserError("فعلاً فقط لینک یوتیوب پشتیبانی می‌شود.")
        }

        val info = fetchInfo(service, url)

        if (info.streamType != StreamType.VIDEO_STREAM &&
            info.streamType != StreamType.POST_LIVE_STREAM
        ) {
            throw UserError("پخش زنده قابل دانلود نیست.")
        }

        val meta = VideoMeta(
            title = info.name ?: "video",
            uploader = info.uploaderName ?: "",
            durationSec = info.duration,
            thumb = info.thumbnails.maxByOrNull { it.height }?.url
        )

        val options = buildOptions(info)
        if (options.isEmpty()) throw UserError("هیچ فرمت قابل دانلودی پیدا نشد.")
        val result = Loaded(meta, options)
        synchronized(this) {
            cacheUrl = url
            cacheAt = System.currentTimeMillis()
            cacheValue = result
        }
        return result
    }

    /** خطاهای موقت (ربات نیستم، شبکه) فقط یک بار با فاصله دوباره امتحان می‌شوند؛ بقیه فوری گزارش می‌شوند. */
    private fun fetchInfo(service: StreamingService, url: String): StreamInfo {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return StreamInfo.getInfo(service, url)
            } catch (e: SignInConfirmNotBotException) {
                if (attempt >= 2) throw UserError(BOT_MSG)
            } catch (e: AgeRestrictedContentException) {
                throw UserError("این ویدیو محدودیت سنی دارد و قابل دریافت نیست.")
            } catch (e: PrivateContentException) {
                throw UserError("این ویدیو خصوصی است.")
            } catch (e: GeographicRestrictionException) {
                throw UserError("این ویدیو در منطقه شما در دسترس نیست.")
            } catch (e: PaidContentException) {
                throw UserError("این ویدیو پولی است و قابل دریافت نیست.")
            } catch (e: YoutubeMusicPremiumContentException) {
                throw UserError("این محتوا فقط برای مشترکان پریمیوم است.")
            } catch (e: ContentNotAvailableException) {
                throw UserError("این ویدیو در دسترس نیست یا حذف شده است.")
            } catch (e: ReCaptchaException) {
                if (attempt >= 2) throw UserError("یوتیوب درخواست‌ها را موقتاً محدود کرده. چند دقیقه صبر کن و دوباره امتحان کن.")
            } catch (e: IOException) {
                if (attempt >= 2) throw UserError("اتصال اینترنت یا فیلترشکن را بررسی کن و دوباره امتحان کن.")
            } catch (e: ParsingException) {
                throw UserError("ویدیویی در این لینک پیدا نشد. لینک خود ویدیو را بده (پلی‌لیست و کانال پشتیبانی نمی‌شود).")
            } catch (e: ExtractionException) {
                throw UserError("دریافت اطلاعات ناموفق بود. ممکن است یوتیوب تغییر کرده باشد؛ نسخه جدید برنامه را بگیر.")
            }
            Thread.sleep(2500)
        }
    }

    private fun Stream.usable(vararg formats: NpFormat): Boolean =
        isUrl && deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && format in formats

    private fun VideoStream.h(): Int =
        height.takeIf { it > 0 }
            ?: Regex("\\d+").find(getResolution())?.value?.toIntOrNull()
            ?: 0

    private fun VideoStream.score(): Long = fps * 1_000_000_000L + bitrate

    private fun label(v: VideoStream): String =
        "${v.h()}p" + if (v.fps > 30) "${v.fps}" else ""

    private fun buildOptions(info: StreamInfo): List<Option> {
        val progressive = info.videoStreams.filter { it.usable(NpFormat.MPEG_4) }
        val videoOnly = info.videoOnlyStreams.filter { it.usable(NpFormat.MPEG_4) }
        val bestAudio = info.audioStreams
            .filter { it.usable(NpFormat.M4A) }
            .maxByOrNull { it.averageBitrate }

        val out = mutableListOf<Option>()
        val heights = (progressive + videoOnly).map { it.h() }.filter { it > 0 }
            .distinct().sortedDescending()

        for (h in heights) {
            val p = progressive.filter { it.h() == h }.maxByOrNull { it.score() }
            if (p != null) {
                val size = sizeOf(p.content).takeIf { it > 0 }
                    ?: if (p.bitrate > 0 && info.duration > 0) p.bitrate / 8L * info.duration else 0L
                out += Option(label(p), "MP4 · تصویر و صدا", size, Kind.PROGRESSIVE, p.content, null)
                continue
            }
            val v = videoOnly.filter { it.h() == h }.maxByOrNull { it.score() }
            if (v != null && bestAudio != null) {
                val vs = sizeOf(v.content)
                val au = sizeOf(bestAudio.content)
                val size = if (vs > 0 && au > 0) vs + au else 0L
                out += Option(
                    label(v), "MP4 · تصویر و صدا (ترکیب روی گوشی)", size,
                    Kind.MERGE, v.content, bestAudio.content
                )
            }
        }

        if (bestAudio != null) {
            out += Option(
                "فقط صدا",
                "M4A · ${bestAudio.averageBitrate} kbps",
                sizeOf(bestAudio.content),
                Kind.AUDIO, null, bestAudio.content
            )
        }
        return out
    }

    private fun sizeOf(url: String): Long = Fetcher.clen(url).coerceAtLeast(0)
}
