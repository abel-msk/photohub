package home.abel.photohub.connector.local;

import home.abel.photohub.connector.HeadersContainer;
import home.abel.photohub.connector.SiteMediaPipe;
import home.abel.photohub.connector.prototype.ExceptionInternalError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;


import java.io.*;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Created by kevin on 10/02/15.
 * See full code here : https://github.com/davinkevin/Podcast-Server/blob/d927d9b8cb9ea1268af74316cd20b7192ca92da7/src/main/java/lan/dk/podcastserver/utils/multipart/MultipartFileSender.java
 */
public class FileRangeRequest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    private static final String DATE_MASK = "EEE, dd MMM yyyy HH:mm:ss zzz";


    private static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

        public static long sublong(String value, int beginIndex, int endIndex) {
            String substring = value.substring(beginIndex, endIndex);
            return (substring.length() > 0) ? Long.parseLong(substring) : -1;
        }
    }


    /**
     *
     *  Open file as inout stream, process generate req/resp headers
     *
     * @param mediaFilePath
     * @param requestHeaders
     * @return
    ; "еркщцы Учсузешщт
     */
    public SiteMediaPipe loadMediaByPath(String mediaFilePath, HeadersContainer requestHeaders)  throws Exception {

        if (requestHeaders == null) {
            requestHeaders = new HeadersContainer();
        }

        SiteMediaPipe response = new SiteMediaPipe();
        File mediaFile = new File(mediaFilePath);
        if ( (! mediaFile.exists()) || ( ! mediaFile.canRead()) ) {
            throw new ExceptionInternalError("Internal Error");
        }

        Path filepath = mediaFile.toPath();
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_MASK);

        Long length = mediaFile.length();
        String fileName = filepath.getFileName().toString();
        FileTime lastModifiedObj = Files.getLastModifiedTime(filepath);
        long lastModified = LocalDateTime.ofInstant(lastModifiedObj.toInstant(),
                ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
        String contentType = LocalSiteConnector.getFileMimeType(mediaFile);


//        Map<String,String> requestHdrs = new  HashMap<>();
//        Map<String,String> responseHdrs = new  HashMap<>();
//
//        if ( headers != null) {
//            String[] inputHeaders = headers.split("\\r?\\n");
//            for ( String hdr : inputHeaders) {
//                if (hdr.contains(":")) {
//                    requestHdrs.put(hdr.substring(0, hdr.indexOf(":")), hdr.substring(hdr.indexOf(":") + 1).trim());
//                }
//            }
//        }

        //-------------------------------------------------------------
        //
        //   Check for header mismatching
        //
        //-------------------------------------------------------------
        InputStream fileIS = null;
        SiteMediaPipe errResp = checkHeaders(mediaFile,requestHeaders);

        //   Has header problems
        if ( errResp != null) {
            response = errResp;
        }
        else {
            //-------------------------------------------------------------
            //
            //   Validate and process range
            //
            //-------------------------------------------------------------

            // Prepare some variables. The full Range represents the complete file.
            Range full = new Range(0, length - 1, length);
            List<Range> ranges = new ArrayList<>();

            // Validate and process Range and If-Range headers.
            String range = requestHeaders.getFirstValue("Range");
            if (range != null) {

                String ifRange = requestHeaders.getFirstValue("If-Range");
                if (ifRange != null && !ifRange.equals(fileName)) {
                    try {
                        long ifRangeTime = httpDateParse(requestHeaders.getFirstValue("If-Range")); // Throws IAE if invalid.
                        if (ifRangeTime != -1) {
                            ranges.add(full);
                        }
                    } catch (IllegalArgumentException ignore) {
                        ranges.add(full);
                    }
                }

                // If any valid If-Range header, then process each part of byte range.
                if (ranges.isEmpty()) {
                    for (String part : range.substring(6).split(",")) {
                        // Assuming a file with length of 100, the following examples returns bytes at:
                        // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                        long start = Range.sublong(part, 0, part.indexOf("-"));
                        long end = Range.sublong(part, part.indexOf("-") + 1, part.length());

                        if (start == -1) {
                            start = length - end;
                            end = length - 1;
                        } else if (end == -1 || end > length - 1) {
                            end = length - 1;
                        }

                        // Check if Range is syntactically valid. If not, then return 416.
    //                        if (start > end) {
    //                            responseHdrs.put("Content-Range", "bytes */" + length); // Required in 416.
    //                            //response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    //                            return;
    //                        }

                        // Add range.
                        ranges.add(new Range(start, end, length));
                    }
                }
            }

            //-------------------------------------------------------------
            //
            //   Prepare  content disposition
            //
            //-------------------------------------------------------------
            String disposition = "inline";  // Get content type by file name and set content disposition.

            if (!contentType.startsWith("image")) {
                // Expect for images, determine content disposition. If content type is supported by
                // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
                String accept = requestHeaders.getFirstValue("Accept");
                disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
            }

            logger.debug("Content-Type : {}", contentType);
            response.addHeader("Content-Type", contentType);
            response.addHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
            logger.debug("Content-Disposition : {}", disposition);
            response.addHeader("Accept-Ranges", "bytes");
            response.addHeader("ETag", fileName);
            response.addHeader("Last-Modified", formatter.format(new Date(lastModified)));
            response.addHeader("Expires", formatter.format(new Date(System.currentTimeMillis() + DEFAULT_EXPIRE_TIME)));


            //-------------------------------------------------------------
            //
            //   Prepare stream path
            //
            //-------------------------------------------------------------
            //fileIS = new FileInputStream(mediaFile);

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                logger.info("Return full file");
                //response.addHeader("Content-Type", contentType);
                response.addHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);
                response.addHeader("Content-Length", String.valueOf(full.length));

                FileInputStream fis = new FileInputStream(mediaFile);
                fis.skip(full.start);
                response.setInputStream(Channels.newInputStream(fis.getChannel()));


            } else if (ranges.size() > 0) {
                // Return single part of file.
                Range r = ranges.get(0);
                logger.info("Return 1 part of file : from ({}) to ({})", r.start, r.end);
                //response.addHeader("Content-Type", contentType);
                response.addHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.addHeader("Content-Length", String.valueOf(r.length));
                response.setStatus("206"); //SC_PARTIAL_CONTENT
                //fileIS.skip(r.start);
                FileInputStream fis = new FileInputStream(mediaFile);
                fis.skip(full.start);
//                response.setInputStream(
//                        Channels.newInputStream(fis.getChannel().truncate(r.start + r.end))
//                );
                response.setInputStream(fis);
            }
        }

        return response;
    }


    /**
     *
     * Check headers context in case http request for full or partial file downloading
     *
     * @param mediaFile
     * @param requestHeaders
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private SiteMediaPipe checkHeaders(File mediaFile, HeadersContainer requestHeaders) throws IOException, ParseException{
        SiteMediaPipe pipe = new SiteMediaPipe();
        String fileName = mediaFile.getName();

        long lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(mediaFile.toPath()).toInstant(), ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);

        //-------------------------------------------------------------
        //
        // Validate request headers for caching
        //
        //-------------------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = requestHeaders.getFirstValue("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, fileName)) {
            pipe.addHeader("ETag", fileName); // Required in 304.
           //response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
           pipe.setError("304");
           return pipe;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        if (requestHeaders.getFirstValue("If-Modified-Since") != null) {
            long ifModifiedSince = httpDateParse(requestHeaders.getFirstValue("If-Modified-Since"));
            if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
                pipe.addHeader("ETag", fileName); // Required in 304.
                //response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                pipe.setError("304");
                return pipe;
            }
        }

        //-------------------------------------------------------------
        //
        // Validate request headers for resume
        //
        //-------------------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = requestHeaders.getFirstValue("If-Match");
        if (ifMatch != null && !matches(ifMatch, fileName)) {
            //response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            pipe.setError("412");
            return pipe;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        if (requestHeaders.getFirstValue("If-Unmodified-Since") != null) {
            long ifUnmodifiedSince = httpDateParse(requestHeaders.getFirstValue("If-Unmodified-Since"));
            if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
                //response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                pipe.setError("412");
                return pipe;
            }
        }

        String range = requestHeaders.getFirstValue("Range");
        if (range != null) {
            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                pipe.addHeader("Content-Range", "bytes */" + mediaFile.length()); // Required in 416.
                //response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                pipe.setError("416");
                return pipe;
            }
        }
        return null;
    }


    /**
     * Parse test date representation, in format used in http headers
     * @param dataStr
     * @return Return milliseconds since 1970
     * @throws ParseException
     */
    private Long httpDateParse(String dataStr) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat(DATE_MASK);
        Date date = parser.parse(dataStr);
        return date.getTime();
    }

    /**
     * Returns true if the given accept header accepts the given value.
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    public static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);

        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    public static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
                || Arrays.binarySearch(matchValues, "*") > -1;
    }

}