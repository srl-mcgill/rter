package ca.nehil.rter.streamingapp;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.DoublePointer;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacpp.IntPointer;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacpp.PointerPointer;
import com.googlecode.javacpp.ShortPointer;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.client.ClientProtocolException;

import android.util.Log;

import com.googlecode.javacv.cpp.avcodec.AVCodecContext;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.avcodec.*;
import static com.googlecode.javacv.cpp.avformat.*;
import static com.googlecode.javacv.cpp.avutil.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.swresample.*;
import static com.googlecode.javacv.cpp.swscale.*;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Encodes images and audio samples into an H264 MPEG-TS stream, and sends as packets over HTTP.
 * @author Samuel Audet
 * @author Michael Sterle-Contala
 */
public class FFmpegFrameSender extends com.googlecode.javacv.FrameRecorder {
	
	public static FFmpegFrameSender createDefault(String f, String t, int w, int h) throws Exception { return new FFmpegFrameSender(f, t, w, h); }
    
    private static Exception loadingException = null;
    
    public static void tryLoad() throws Exception {
        if (loadingException != null) {
            throw loadingException;
        } else {
            try {
                Loader.load(com.googlecode.javacv.cpp.avutil.class);
                Loader.load(com.googlecode.javacv.cpp.avcodec.class);
                Loader.load(com.googlecode.javacv.cpp.avformat.class);
                Loader.load(com.googlecode.javacv.cpp.swscale.class);
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw loadingException = (Exception)t;
                } else {
                    throw loadingException = new Exception("Failed to load " + FFmpegFrameSender.class, t);
                }
            }
        }
    }
    
    /**
     * Constructor for video only (no audio).
     * 
     * @param	rterResource	URL for videoserver endpoint to send to
     * @param	rterToken		authorization token for videoserver
     * @param	imageWidth		image width in pixels
     * @param	imageHeight		image height in pixels
     */
    public FFmpegFrameSender(String rterResource, String rterToken, int imageWidth, int imageHeight) {
        this(rterResource, rterToken, imageWidth, imageHeight, 0);
    }
    
    /**
     * Constructor for video with audio.
     * 
     * @param	rterResource	URL for videoserver endpoint to send to
     * @param	rterToken		authorization token for videoserver
     * @param	imageWidth		image width in pixels
     * @param	imageHeight		image height in pixels
     * @param	audioChannels	number of audio channels
     */
    public FFmpegFrameSender(String rterResource, String rterToken, int imageWidth, int imageHeight, int audioChannels) {
        /* initialize libavcodec, and register all codecs and formats */
        av_register_all();
        avformat_network_init();

        this.filename      = "/mnt/sdcard/stream.ts";
        this.RterResourceSet = rterResource;
        this.RterAuthToken = rterToken;
        this.imageWidth    = imageWidth;
        this.imageHeight   = imageHeight;
        this.audioChannels = audioChannels;
        this.pixelFormat   = 0; // PIX_FMT_YUV420P
        this.videoCodec    = AV_CODEC_ID_H264;
        this.videoBitrate  = 400000;
        this.frameRate     = 30;

        this.sampleFormat  = AV_SAMPLE_FMT_NONE;
        this.audioCodec    = AV_CODEC_ID_NONE;
        this.audioBitrate  = 64000;
        this.sampleRate    = 44100;

        this.interleaved = true;

        this.video_pkt = new AVPacket();
        this.audio_pkt = new AVPacket();
        
        this.postqueue = new LinkedBlockingQueue<HttpPost>((int) (3 * this.frameRate));
        this.httpThread = new Thread(new HttpSender());
        this.httpThread.start();
    }
    
    /**
     * Closes and frees all resources.
     * This method is called by the {@link #stop()} method
     */
    public void release() throws Exception {
        /* close each codec */
        if (video_c != null) {
            avcodec_close(video_c);
            video_c = null;
        }
        if (audio_c != null) {
            avcodec_close(audio_c);
            audio_c = null;
        }
        if (picture_buf != null) {
            av_free(picture_buf);
            picture_buf = null;
        }
        if (picture != null) {
            avcodec_free_frame(picture);
            picture = null;
        }
        if (tmp_picture != null) {
            avcodec_free_frame(tmp_picture);
            tmp_picture = null;
        }
        if (video_outbuf != null) {
            av_free(video_outbuf);
            video_outbuf = null;
        }
        if (frame != null) {
            avcodec_free_frame(frame);
            frame = null;
        }
        if (samples_out != null) {
            for (int i = 0; i < samples_out.length; i++) {
                av_free(samples_out[i].position(0));
            }
            samples_out = null;
        }
        if (audio_outbuf != null) {
            av_free(audio_outbuf);
            audio_outbuf = null;
        }
        video_st = null;
        audio_st = null;

        if (oc != null) {
            /* free the streams */
            int nb_streams = oc.nb_streams();
            for(int i = 0; i < nb_streams; i++) {
                av_free(oc.streams(i).codec());
                av_free(oc.streams(i));
            }

            /* free the stream */
            av_free(oc);
            oc = null;
        }

        if (img_convert_ctx != null) {
            sws_freeContext(img_convert_ctx);
            img_convert_ctx = null;
        }

        if (samples_convert_ctx != null) {
            swr_free(samples_convert_ctx);
            samples_convert_ctx = null;
        }
        
    }
    
    /**
     * Closes and frees all resources.
     */
    @Override protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    private int frame_number;
    private String filename;
    private String RterAuthToken;
    private String RterResourceSet;
    private AVFrame picture, tmp_picture;
    private BytePointer picture_buf;
    private BytePointer video_outbuf;
    private int video_outbuf_size;
    private AVFrame frame;
    private Pointer[] samples_in;
    private BytePointer[] samples_out;
    private PointerPointer samples_in_ptr;
    private PointerPointer samples_out_ptr;
    private BytePointer audio_outbuf;
    private int audio_outbuf_size;
    private int audio_input_frame_size;
    private AVOutputFormat oformat;
    private AVFormatContext oc;
    private AVCodec video_codec, audio_codec;
    private AVCodecContext video_c, audio_c;
    private AVStream video_st, audio_st;
    private SwsContext img_convert_ctx;
    private SwrContext samples_convert_ctx;
    private AVPacket video_pkt, audio_pkt;
    private int[] got_video_packet, got_audio_packet;
    private final LinkedBlockingQueue<HttpPost> postqueue;
    private Thread httpThread;
    private AVIOContext io_context;

    @Override public int getFrameNumber() {
        return picture == null ? super.getFrameNumber() : (int)picture.pts();
    }
    @Override public void setFrameNumber(int frameNumber) {
        if (picture == null) { super.setFrameNumber(frameNumber); } else { picture.pts(frameNumber); }
    }

    // best guess for timestamp in microseconds...
    @Override public long getTimestamp() {
        return Math.round(getFrameNumber() * 1000000L / getFrameRate());
    }
    @Override public void setTimestamp(long timestamp)  {
        setFrameNumber((int)Math.round(timestamp * getFrameRate() / 1000000L));
    }

    /**
     * Start a stream.
     */
    public void start() throws Exception {
        int ret;
        this.frame_number = 0;
        picture = null;
        tmp_picture = null;
        picture_buf = null;
        frame = null;
        video_outbuf = null;
        audio_outbuf = null;
        oc = null;
        video_c = null;
        audio_c = null;
        video_st = null;
        audio_st = null;
        got_video_packet = new int[1];
        got_audio_packet = new int[1];

        /* auto detect the output format from the name. */
        String format_name = format == null || format.length() == 0 ? null : format;
        if ((oformat = av_guess_format("mpegts", null, null)) == null) {
            int proto = filename.indexOf("://");
            if (proto > 0) {
                format_name = filename.substring(0, proto);
            }
            if ((oformat = av_guess_format(format_name, filename, null)) == null) {
                throw new Exception("av_guess_format() error: Could not guess output format for \"" + filename + "\" and " + format + " format.");
            }
        }
        format_name = oformat.name().getString();

        /* allocate the output media context */
        if ((oc = avformat_alloc_context()) == null) {
            throw new Exception("avformat_alloc_context() error: Could not allocate format context");
        }
        
        oc.oformat(oformat);

        /* add the audio and video streams using the format codecs
           and initialize the codecs */

        if (imageWidth > 0 && imageHeight > 0) {
            if (videoCodec != AV_CODEC_ID_NONE) {
                oformat.video_codec(videoCodec);
            } else if ("flv".equals(format_name)) {
                oformat.video_codec(AV_CODEC_ID_FLV1);
            } else if ("mp4".equals(format_name)) {
                oformat.video_codec(AV_CODEC_ID_MPEG4);
            } else if ("3gp".equals(format_name)) {
                oformat.video_codec(AV_CODEC_ID_H263);
            } else if ("avi".equals(format_name)) {
                oformat.video_codec(AV_CODEC_ID_HUFFYUV);
            }

            /* find the video encoder */
            if ((video_codec = avcodec_find_encoder(oformat.video_codec())) == null) {
                release();
                throw new Exception("avcodec_find_encoder() error: Video codec not found.");
            }

            AVRational frame_rate = av_d2q(frameRate, 1001000);
            AVRational supported_framerates = video_codec.supported_framerates();
            if (supported_framerates != null) {
                int idx = av_find_nearest_q_idx(frame_rate, supported_framerates);
                frame_rate = supported_framerates.position(idx);
            }

            /* add a video output stream */
            if ((video_st = avformat_new_stream(oc, video_codec)) == null) {
                release();
                throw new Exception("avformat_new_stream() error: Could not allocate video stream.");
            }
            video_c = video_st.codec();
            video_c.codec_id(oformat.video_codec());
            video_c.codec_type(AVMEDIA_TYPE_VIDEO);

            /* put sample parameters */
            video_c.bit_rate(videoBitrate);
            /* resolution must be a multiple of two, but round up to 16 as often required */
            video_c.width((imageWidth + 15) / 16 * 16);
            video_c.height(imageHeight);
            /* time base: this is the fundamental unit of time (in seconds) in terms
               of which frame timestamps are represented. for fixed-fps content,
               timebase should be 1/framerate and timestamp increments should be
               identically 1. */
            video_c.time_base(av_inv_q(frame_rate));
            video_c.gop_size(30);
            if (videoQuality >= 0) {
                video_c.flags(video_c.flags() | CODEC_FLAG_QSCALE);
                video_c.global_quality((int)Math.round(FF_QP2LAMBDA * videoQuality));
            }

            if (pixelFormat != AV_PIX_FMT_NONE) {
                video_c.pix_fmt(pixelFormat);
            } else if (video_c.codec_id() == AV_CODEC_ID_RAWVIDEO || video_c.codec_id() == AV_CODEC_ID_PNG ||
                       video_c.codec_id() == AV_CODEC_ID_HUFFYUV  || video_c.codec_id() == AV_CODEC_ID_FFV1) {
                video_c.pix_fmt(AV_PIX_FMT_RGB32);   // appropriate for common lossless formats
            } else {
                video_c.pix_fmt(AV_PIX_FMT_YUV420P); // lossy, but works with about everything
            }

            if (video_c.codec_id() == AV_CODEC_ID_MPEG2VIDEO) {
                /* just for testing, we also add B frames */
                video_c.max_b_frames(2);
            } else if (video_c.codec_id() == AV_CODEC_ID_MPEG1VIDEO) {
                /* Needed to avoid using macroblocks in which some coeffs overflow.
                   This does not happen with normal video, it just happens here as
                   the motion of the chroma plane does not match the luma plane. */
                video_c.mb_decision(2);
            } else if (video_c.codec_id() == AV_CODEC_ID_H263) {
                // H.263 does not support any other resolution than the following
                if (imageWidth <= 128 && imageHeight <= 96) {
                    video_c.width(128).height(96);
                } else if (imageWidth <= 176 && imageHeight <= 144) {
                    video_c.width(176).height(144);
                } else if (imageWidth <= 352 && imageHeight <= 288) {
                    video_c.width(352).height(288);
                } else if (imageWidth <= 704 && imageHeight <= 576) {
                    video_c.width(704).height(576);
                } else {
                    video_c.width(1408).height(1152);
                }
            } else if (video_c.codec_id() == AV_CODEC_ID_H264) {
                // default to constrained baseline to produce content that plays back on anything,
                // without any significant tradeoffs for most use cases
                video_c.profile(AVCodecContext.FF_PROFILE_H264_BASELINE);
                video_c.max_b_frames(0);
                video_c.refs(1);
                video_c.keyint_min(15 * 2); //keyint=<framerate * segment length>
                video_c.level(30);
                video_c.bit_rate_tolerance(0);
                video_c.rc_buffer_size(video_c.bit_rate());
                video_c.rc_max_rate(video_c.bit_rate());
                video_c.rc_min_rate(0);
                video_c.thread_count(1);
                video_c.trellis(0);
            }

            // some formats want stream headers to be separate
            if ((oformat.flags() & AVFMT_GLOBALHEADER) != 0) {
                video_c.flags(video_c.flags() | CODEC_FLAG_GLOBAL_HEADER);
            }

            if ((video_codec.capabilities() & CODEC_CAP_EXPERIMENTAL) != 0) {
                video_c.strict_std_compliance(AVCodecContext.FF_COMPLIANCE_EXPERIMENTAL);
            }
        }

        /*
         * add an audio output stream
         */
        if (audioChannels > 0 && audioBitrate > 0 && sampleRate > 0) {
            if (audioCodec != AV_CODEC_ID_NONE) {
                oformat.audio_codec(audioCodec);
            } else if ("flv".equals(format_name) || "mp4".equals(format_name) || "3gp".equals(format_name)) {
                oformat.audio_codec(AV_CODEC_ID_AAC);
            } else if ("avi".equals(format_name)) {
                oformat.audio_codec(AV_CODEC_ID_PCM_S16LE);
            }

            /* find the audio encoder */
            if ((audio_codec = avcodec_find_encoder(AV_CODEC_ID_H264)) == null) {
                release();
                throw new Exception("avcodec_find_encoder() error: Audio codec not found.");
            }

            if ((audio_st = avformat_new_stream(oc, audio_codec)) == null) {
                release();
                throw new Exception("avformat_new_stream() error: Could not allocate audio stream.");
            }
            audio_c = audio_st.codec();
            audio_c.codec_id(oformat.audio_codec());
            audio_c.codec_type(AVMEDIA_TYPE_AUDIO);

            /* put sample parameters */
            audio_c.bit_rate(audioBitrate);
            audio_c.sample_rate(sampleRate);
            audio_c.channels(audioChannels);
            audio_c.channel_layout(av_get_default_channel_layout(audioChannels));
            if (sampleFormat != AV_SAMPLE_FMT_NONE) {
                audio_c.sample_fmt(sampleFormat);
            } else if (audio_c.codec_id() == AV_CODEC_ID_AAC &&
                    (audio_codec.capabilities() & CODEC_CAP_EXPERIMENTAL) != 0) {
                audio_c.sample_fmt(AV_SAMPLE_FMT_FLTP);
            } else {
                audio_c.sample_fmt(AV_SAMPLE_FMT_S16);
            }
            audio_c.time_base().num(1).den(sampleRate);
            switch (audio_c.sample_fmt()) {
                case AV_SAMPLE_FMT_U8:
                case AV_SAMPLE_FMT_U8P:  audio_c.bits_per_raw_sample(8);  break;
                case AV_SAMPLE_FMT_S16:
                case AV_SAMPLE_FMT_S16P: audio_c.bits_per_raw_sample(16); break;
                case AV_SAMPLE_FMT_S32:
                case AV_SAMPLE_FMT_S32P: audio_c.bits_per_raw_sample(32); break;
                case AV_SAMPLE_FMT_FLT:
                case AV_SAMPLE_FMT_FLTP: audio_c.bits_per_raw_sample(32); break;
                case AV_SAMPLE_FMT_DBL:
                case AV_SAMPLE_FMT_DBLP: audio_c.bits_per_raw_sample(64); break;
                default: assert false;
            }
            if (audioQuality >= 0) {
                audio_c.flags(audio_c.flags() | CODEC_FLAG_QSCALE);
                audio_c.global_quality((int)Math.round(FF_QP2LAMBDA * audioQuality));
            }

            // some formats want stream headers to be separate
            if ((oformat.flags() & AVFMT_GLOBALHEADER) != 0) {
                audio_c.flags(audio_c.flags() | CODEC_FLAG_GLOBAL_HEADER);
            }

            if ((audio_codec.capabilities() & CODEC_CAP_EXPERIMENTAL) != 0) {
                audio_c.strict_std_compliance(AVCodecContext.FF_COMPLIANCE_EXPERIMENTAL);
            }
        }
        
        av_dump_format(oc, 0, filename, 1);

        /* now that all the parameters are set, we can open the audio and
           video codecs and allocate the necessary encode buffers */
        if (video_st != null) {
            AVDictionary options = new AVDictionary(null);
            if (videoQuality >= 0) {
                av_dict_set(options, "crf", "" + videoQuality, 0);
            }
            for (Entry<String, String> e : videoOptions.entrySet()) {
                av_dict_set(options, e.getKey(), e.getValue(), 0);
            }
            av_dict_set(options, "preset", "superfast", 0);
            //av_dict_set(options, "tune", "zerolatency", 0);
            av_dict_set(options, "vprofile", "baseline", 0);
            av_dict_set(options, "crf", "10", 0);
            av_dict_set(options, "hrd", "crf", 0);
            av_dict_set(options, "lookahead", "0", 0);
            av_dict_set(options, "ratetol", "1.0", 0);
            av_dict_set(options, "keyint", "30", 0);
            av_dict_set(options, "bframes", "0", 0);
            av_dict_set(options, "ref", "1", 0);
            av_dict_set(options, "hrd", "cbr", 0);
            av_dict_set(options, "b", String.valueOf(this.videoBitrate / 1000), 0);
            /* open the codec */
            if ((ret = avcodec_open2(video_c, video_codec, options)) < 0) {
                release();
                throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
            }
            av_dict_free(options);

            video_outbuf = null;
            if ((oformat.flags() & AVFMT_RAWPICTURE) == 0) {
                /* allocate output buffer */
                /* XXX: API change will be done */
                /* buffers passed into lav* can be allocated any way you prefer,
                   as long as they're aligned enough for the architecture, and
                   they're freed appropriately (such as using av_free for buffers
                   allocated with av_malloc) */
                video_outbuf_size = Math.max(256 * 1024, 8 * video_c.width() * video_c.height()); // a la ffmpeg.c
                video_outbuf = new BytePointer(av_malloc(video_outbuf_size));
            }

            /* allocate the encoded raw picture */
            if ((picture = avcodec_alloc_frame()) == null) {
                release();
                throw new Exception("avcodec_alloc_frame() error: Could not allocate picture.");
            }
            picture.pts(0); // magic required by libx264

            int size = avpicture_get_size(video_c.pix_fmt(), video_c.width(), video_c.height());
            if ((picture_buf = new BytePointer(av_malloc(size))).isNull()) {
                release();
                throw new Exception("av_malloc() error: Could not allocate picture buffer.");
            }

            /* if the output format is not equal to the image format, then a temporary
               picture is needed too. It is then converted to the required output format */
            if ((tmp_picture = avcodec_alloc_frame()) == null) {
                release();
                throw new Exception("avcodec_alloc_frame() error: Could not allocate temporary picture.");
            }
        }

        if (audio_st != null) {
            AVDictionary options = new AVDictionary(null);
            if (audioQuality >= 0) {
                av_dict_set(options, "crf", "" + audioQuality, 0);
            }
            for (Entry<String, String> e : audioOptions.entrySet()) {
                av_dict_set(options, e.getKey(), e.getValue(), 0);
            }
            /* open the codec */
            if ((ret = avcodec_open2(audio_c, audio_codec, options)) < 0) {
                release();
                throw new Exception("avcodec_open2() error " + ret + ": Could not open audio codec.");
            }
            av_dict_free(options);

            audio_outbuf_size = 256 * 1024;
            audio_outbuf = new BytePointer(av_malloc(audio_outbuf_size));

            /* ugly hack for PCM codecs (will be removed ASAP with new PCM
               support to compute the input frame size in samples */
            if (audio_c.frame_size() <= 1) {
                audio_outbuf_size = FF_MIN_BUFFER_SIZE;
                audio_input_frame_size = audio_outbuf_size / audio_c.channels();
                switch (audio_c.codec_id()) {
                    case AV_CODEC_ID_PCM_S16LE:
                    case AV_CODEC_ID_PCM_S16BE:
                    case AV_CODEC_ID_PCM_U16LE:
                    case AV_CODEC_ID_PCM_U16BE:
                        audio_input_frame_size >>= 1;
                        break;
                    default:
                        break;
                }
            } else {
                audio_input_frame_size = audio_c.frame_size();
            }
            //int bufferSize = audio_input_frame_size * audio_c.bits_per_raw_sample()/8 * audio_c.channels();
            int planes = av_sample_fmt_is_planar(audio_c.sample_fmt()) != 0 ? (int)audio_c.channels() : 1;
            int data_size = av_samples_get_buffer_size(null, audio_c.channels(),
                    audio_input_frame_size, audio_c.sample_fmt(), 1) / planes;
            samples_out = new BytePointer[planes];
            for (int i = 0; i < samples_out.length; i++) {
                samples_out[i] = new BytePointer(av_malloc(data_size)).capacity(data_size);
            }
            samples_in = new Pointer[AVFrame.AV_NUM_DATA_POINTERS];
            samples_in_ptr  = new PointerPointer(AVFrame.AV_NUM_DATA_POINTERS);
            samples_out_ptr = new PointerPointer(AVFrame.AV_NUM_DATA_POINTERS);

            /* allocate the audio frame */
            if ((frame = avcodec_alloc_frame()) == null) {
                release();
                throw new Exception("avcodec_alloc_frame() error: Could not allocate audio frame.");
            }
        }

        // Custom I/O context
    	AVIOContext.Write_packet write_packet = new AVIOContext.Write_packet() {
    	    @Override
    	    public int call(Pointer opaque, BytePointer buf, int buf_size) {
    	    	byte[] bbuf = new byte[buf_size];
    	    	buf.get(bbuf);
//    	    	Log.d("FFMPEG", "write_packet: " + String.valueOf(buf_size) + " bytes");
    	    	
    	    	HttpPost httpPost = new HttpPost(RterResourceSet + "/ts");
                httpPost.setHeader("Authorization", RterAuthToken);
                httpPost.setEntity(new ByteArrayEntity(bbuf));
                
                try {
                	postqueue.put(httpPost);
                } catch (InterruptedException e) {
                	e.printStackTrace();
                }
    	    	
    	    	return buf_size;
    	    }
    	};
    	
    	AVIOContext pb = avio_alloc_context(new BytePointer(av_malloc(32 * 1024)), 32 * 1024, 1, null, null, write_packet, null);
        pb.seekable(0);
    	oc.pb(pb);

        /* write the stream header, if any */
        avformat_write_header(oc, null);
 
    }

    /**
     * End a stream.
     */
    public void stop() throws Exception {
        if (oc != null) {
            /* write the trailer, if any */
            av_write_trailer(oc);
        }
        release();
    }

    /**
     * Add an image to the stream.
     * 
     * @param image			image to add to the stream
     * @throws Exception	if pixel format could not be guessed
     */
    public void record(IplImage image) throws Exception {
        record(image, AV_PIX_FMT_NONE);
    }
    
    /**
     * Add an image to the stream.
     * 
     * @param image			image to add to the stream
     * @param pixelFormat	pixel format as defined in {@link com.googlecode.javacv.cpp.avutil#AV_PICTURE_TYPE_B com.googlecode.javacv.cpp.avutil}
     * @throws Exception	if pixel format could not be guessed
     */
    public void record(IplImage image, int pixelFormat) throws Exception {
        if (video_st == null) {
            throw new Exception("No video output stream (Is imageWidth > 0 && imageHeight > 0 and has start() been called?)");
        }
        int ret;

        if (image == null) {
            // no more frame to compress. The codec has a latency of a few
            // frames if using B frames, so we get the last frames by
            // passing the same picture again 
        } else {
            int width = image.width();
            int height = image.height();
            int step = image.widthStep();
            
            BytePointer data = image.imageData();

            if (pixelFormat == AV_PIX_FMT_NONE) {
                int depth = image.depth();
                int channels = image.nChannels();
                if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 3) {
                    pixelFormat = AV_PIX_FMT_BGR24;
                } else if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 1) {
                    pixelFormat = AV_PIX_FMT_GRAY8;
                } else if ((depth == IPL_DEPTH_16U || depth == IPL_DEPTH_16S) && channels == 1) {
                    pixelFormat = AV_HAVE_BIGENDIAN() ? AV_PIX_FMT_GRAY16BE : AV_PIX_FMT_GRAY16LE;
                } else if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 4) {
                    pixelFormat = AV_PIX_FMT_RGBA;
                } else if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 2) {
                    pixelFormat = AV_PIX_FMT_NV21; // Android's camera capture format
                    step = width;
                } else {
                    throw new Exception("Could not guess pixel format of image: depth=" + depth + ", channels=" + channels);
                }
            }

            if (video_c.pix_fmt() != pixelFormat || video_c.width() != width || video_c.height() != height) {
                // convert to the codec pixel format if needed
                img_convert_ctx = sws_getCachedContext(img_convert_ctx, width, height, pixelFormat,
                        video_c.width(), video_c.height(), video_c.pix_fmt(), SWS_BILINEAR, null, null, null);
                if (img_convert_ctx == null) {
                    throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
                }
                avpicture_fill(tmp_picture, data, pixelFormat, width, height);
                avpicture_fill(picture, picture_buf, video_c.pix_fmt(), video_c.width(), video_c.height());
                tmp_picture.linesize(0, step);
                sws_scale(img_convert_ctx, new PointerPointer(tmp_picture), tmp_picture.linesize(),
                          0, height, new PointerPointer(picture), picture.linesize());
            } else {
                avpicture_fill(picture, data, pixelFormat, width, height);
                picture.linesize(0, step);
            }
        }

        video_pkt.flags(video_pkt.flags() | AV_PKT_FLAG_KEY);
        
        if ((oformat.flags() & AVFMT_RAWPICTURE) != 0) {
            // raw video case. The API may change slightly in the future for that?
            av_init_packet(video_pkt);
            video_pkt.flags(video_pkt.flags() | AV_PKT_FLAG_KEY);
            video_pkt.stream_index(video_st.index());
            video_pkt.data(new BytePointer(picture));
            video_pkt.size(Loader.sizeof(AVPicture.class));
        } else {
            // encode the image
            av_init_packet(video_pkt);
            video_pkt.data(video_outbuf);
            video_pkt.size(video_outbuf_size);
            picture.quality(video_c.global_quality());
            if ((ret = avcodec_encode_video2(video_c, video_pkt, picture, got_video_packet)) < 0) {
                throw new Exception("avcodec_encode_video2() error " + ret + ": Could not encode video packet.");
            }
            picture.pts(picture.pts() + 1); // magic required by libx264
            // if zero size, it means the image was buffered
            if (got_video_packet[0] != 0) {
                if (video_pkt.pts() != AV_NOPTS_VALUE) {
                    video_pkt.pts(av_rescale_q(video_pkt.pts(), video_c.time_base(), video_st.time_base()));
                }
                if (video_pkt.dts() != AV_NOPTS_VALUE) {
                    video_pkt.dts(av_rescale_q(video_pkt.dts(), video_c.time_base(), video_st.time_base()));
                }
                video_pkt.stream_index(video_st.index());
            } else {
                return;
            }
            Log.i("MSC", "PTS: " + String.valueOf(video_pkt.pts()));
            
        }

        synchronized (oc) {
            // write the compressed frame to the I/O context
            if (interleaved && audio_st != null) {
                if ((ret = av_interleaved_write_frame(oc, video_pkt)) < 0) {
                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved video frame.");
                }
            } else {
                if ((ret = av_write_frame(oc, video_pkt)) < 0) {
                    throw new Exception("av_write_frame() error " + ret + " while writing video frame. s->pb->error=" + String.valueOf(oc.pb().error()));
                }
            }
        }
    }

    /**
     * Add audio samples to the stream.
     * 
     * @param	samples		audio samples to add to stream
     * @throws	Exception	on error
     */
    @Override
    public void record(Buffer[] samples) throws Exception {
        if (audio_st == null) {
            throw new Exception("No audio output stream (Is audioChannels > 0 and has start() been called?)");
        }
        int ret;

        int inputSize = samples[0].limit() - samples[0].position();
        int inputFormat = AV_SAMPLE_FMT_NONE;
        int inputChannels = samples.length > 1 ? 1 : audioChannels;
        int inputDepth = 0;
        int outputFormat = audio_c.sample_fmt();
        int outputChannels = samples_out.length > 1 ? 1 : audioChannels;
        int outputDepth = av_get_bytes_per_sample(outputFormat);
        if (samples[0] instanceof ByteBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_U8P : AV_SAMPLE_FMT_U8;
            inputDepth = 1;
            for (int i = 0; i < samples.length; i++) {
                ByteBuffer b = (ByteBuffer)samples[i];
                if (samples_in[i] instanceof BytePointer && samples_in[i].capacity() >= inputSize && b.hasArray()) {
                    ((BytePointer)samples_in[i]).position(0).put(b.array(), b.position(), inputSize);
                } else {
                    samples_in[i] = new BytePointer(b);
                }
            }
        } else if (samples[0] instanceof ShortBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_S16P : AV_SAMPLE_FMT_S16;
            inputDepth = 2;
            for (int i = 0; i < samples.length; i++) {
                ShortBuffer b = (ShortBuffer)samples[i];
                if (samples_in[i] instanceof ShortPointer && samples_in[i].capacity() >= inputSize && b.hasArray()) {
                    ((ShortPointer)samples_in[i]).position(0).put(b.array(), samples[i].position(), inputSize);
                } else {
                    samples_in[i] = new ShortPointer(b);
                }
            }
        } else if (samples[0] instanceof IntBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_S32P : AV_SAMPLE_FMT_S32;
            inputDepth = 4;
            for (int i = 0; i < samples.length; i++) {
                IntBuffer b = (IntBuffer)samples[i];
                if (samples_in[i] instanceof IntPointer && samples_in[i].capacity() >= inputSize && b.hasArray()) {
                    ((IntPointer)samples_in[i]).position(0).put(b.array(), samples[i].position(), inputSize);
                } else {
                    samples_in[i] = new IntPointer(b);
                }
            }
        } else if (samples[0] instanceof FloatBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_FLTP : AV_SAMPLE_FMT_FLT;
            inputDepth = 4;
            for (int i = 0; i < samples.length; i++) {
                FloatBuffer b = (FloatBuffer)samples[i];
                if (samples_in[i] instanceof FloatPointer && samples_in[i].capacity() >= inputSize && b.hasArray()) {
                    ((FloatPointer)samples_in[i]).position(0).put(b.array(), b.position(), inputSize);
                } else {
                    samples_in[i] = new FloatPointer(b);
                }
            }
        } else if (samples[0] instanceof DoubleBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_DBLP : AV_SAMPLE_FMT_DBL;
            inputDepth = 8;
            for (int i = 0; i < samples.length; i++) {
                DoubleBuffer b = (DoubleBuffer)samples[i];
                if (samples_in[i] instanceof DoublePointer && samples_in[i].capacity() >= inputSize && b.hasArray()) {
                    ((DoublePointer)samples_in[i]).position(0).put(b.array(), b.position(), inputSize);
                } else {
                    samples_in[i] = new DoublePointer(b);
                }
            }
        } else {
            throw new Exception("Audio samples Buffer has unsupported type: " + samples);
        }

        if (samples_convert_ctx == null) {
            samples_convert_ctx = swr_alloc_set_opts(null,
                    audio_c.channel_layout(), outputFormat, audio_c.sample_rate(),
                    audio_c.channel_layout(), inputFormat,  audio_c.sample_rate(), 0, null);
            if (samples_convert_ctx == null) {
                throw new Exception("swr_alloc_set_opts() error: Cannot allocate the conversion context.");
            } else if ((ret = swr_init(samples_convert_ctx)) < 0) {
                throw new Exception("swr_init() error " + ret + ": Cannot initialize the conversion context.");
            }
        }

        for (int i = 0; i < samples.length; i++) {
            samples_in[i].position(samples_in[i].position() * inputDepth).
                    limit((samples_in[i].position() + inputSize) * inputDepth);
        }
        while (samples_in[0].position() < samples_in[0].limit()) {
            int inputCount = (samples_in[0].limit() - samples_in[0].position()) / (inputChannels * inputDepth);
            int outputCount = (samples_out[0].limit() - samples_out[0].position()) / (outputChannels * outputDepth);
            int count = Math.min(inputCount, outputCount);
            for (int i = 0; i < samples.length; i++) {
                samples_in_ptr.put(i, samples_in[i]);
            }
            for (int i = 0; i < samples_out.length; i++) {
                samples_out_ptr.put(i, samples_out[i]);
            }
            if ((ret = swr_convert(samples_convert_ctx, samples_out_ptr, count, samples_in_ptr, count)) < 0) {
                throw new Exception("swr_convert() error " + ret + ": Cannot convert audio samples.");
            }
            for (int i = 0; i < samples.length; i++) {
                samples_in[i].position(samples_in[i].position() + ret * inputChannels * inputDepth);
            }
            for (int i = 0; i < samples_out.length; i++) {
                samples_out[i].position(samples_out[i].position() + ret * outputChannels * outputDepth);
            }

            if (samples_out[0].position() >= samples_out[0].limit()) {
                frame.nb_samples(audio_input_frame_size);
                avcodec_fill_audio_frame(frame, audio_c.channels(), outputFormat, samples_out[0], samples_out[0].limit(), 0);
                for (int i = 0; i < samples_out.length; i++) {
                    frame.data(i, samples_out[i].position(0));
                    frame.linesize(i, samples_out[i].limit());
                }

                av_init_packet(audio_pkt);
                audio_pkt.data(audio_outbuf);
                audio_pkt.size(audio_outbuf_size);
                frame.quality(audio_c.global_quality());
                if ((ret = avcodec_encode_audio2(audio_c, audio_pkt, frame, got_audio_packet)) < 0) {
                    throw new Exception("avcodec_encode_audio2() error " + ret + ": Could not encode audio packet.");
                }
                if (got_audio_packet[0] != 0) {
                    if (audio_pkt.pts() != AV_NOPTS_VALUE) {
                        audio_pkt.pts(av_rescale_q(audio_pkt.pts(), audio_c.time_base(), audio_c.time_base()));
                    }
                    if (audio_pkt.dts() != AV_NOPTS_VALUE) {
                        audio_pkt.dts(av_rescale_q(audio_pkt.dts(), audio_c.time_base(), audio_c.time_base()));
                    }
                    audio_pkt.flags(audio_pkt.flags() | AV_PKT_FLAG_KEY);
                    audio_pkt.stream_index(audio_st.index());

                    /* write the compressed frame in the media file */
                    synchronized (oc) {
                        if (interleaved && video_st != null) {
                            if ((ret = av_interleaved_write_frame(oc, audio_pkt)) < 0) {
                                throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved audio frame.");
                            }
                        } else {
                            if ((ret = av_write_frame(oc, audio_pkt)) < 0) {
                                throw new Exception("av_write_frame() error " + ret + " while writing audio frame.");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Asynchronous http client. Pulls requests off the queue and sends via HTTP.
     * 
     * @author Michael Sterle-Contala
     *
     */
    class HttpSender implements Runnable{

    	private DefaultHttpClient httpclient;
    	
    	/**
    	 * Sole constructor.
    	 */
        public HttpSender() {
        	this.httpclient = new DefaultHttpClient();
        }
        
        /**
         * Start sending requests.
         */
        @Override
        public void run() {
            while(true){
                try {
                	HttpResponse response = this.httpclient.execute(FFmpegFrameSender.this.postqueue.take());
                	response.getEntity().consumeContent();
                	Log.i("MSC", "Response status " + response.getStatusLine().getStatusCode() + " (" + response.getStatusLine().getReasonPhrase() + ")");
                } catch (InterruptedException e) {
                	e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //this.httpclient.getConnectionManager().shutdown();
        }
    }
    
}