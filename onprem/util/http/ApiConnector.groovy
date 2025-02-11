package utils.http

import javax.ws.rs.core.HttpHeaders

import groovyx.net.http.Method
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator

import org.apache.http.HttpEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import groovy.util.logging.Slf4j

@Slf4j
class ApiConnector {

    private String baseUrl
    
    private HTTPBuilder httpBuilder

    public List<String> cookies

    private Map<String,String> defaultHeaders

    ApiConnector(String url) {
        this.baseUrl = url
        this.httpBuilder = initializeHttpBuilder()
        this.cookies = []
        this.defaultHeaders = [:]
    }
    
    private HTTPBuilder initializeHttpBuilder() {
        HTTPBuilder httpBuilder = new HTTPBuilder(baseUrl)
        httpBuilder.handler.success = { HttpResponseDecorator resp, reader ->
            resp.getHeaders(HttpHeaders.SET_COOKIE).each {
                String cookie = it.value.split(';')[0]
                //log.info("Adding cookie to collection: $cookie")
                cookies.add(cookie)
            }
            //log.info("Response: ${reader}")
            return reader
        }
        return httpBuilder
    }
    /**
     * Adds header to all requests
     * 
     * Uses: 
     * - Authorization, if needed for all requests
     * - Content type setting
     * - Other specific cases when some header should always be set
     */
    void addDefaultHeader(String name, String value){
        this.defaultHeaders[name] = value
    }
    /**
     * Basic method to send request with any method, key-value or json body and any number of url parameters
     */
    def request(Method method, ContentType contentType, String path, Map<String, Serializable> requestBody, Map<String, Serializable> params, Closure failureCallback = null) {
        //log.info("Send $method request to ${this.baseUrl}$path: $params")

        httpBuilder.request(method) { request ->
            requestContentType = contentType
            uri.path = path
            uri.query = params
            if(requestBody){
                body = requestBody
            }
            this.defaultHeaders.each{ String key, String value ->
                log.info('Add default header {}', key)
                headers[key] = value
            }
            headers[HttpHeaders.COOKIE] = cookies.join(';')
            if (contentType == ContentType.BINARY) {
                response.'200' = { resp, binary ->
                    return binary.bytes //binary in this case is an instance of InputStream
                }
            } else if(contentType == ContentType.TEXT){
                response.'200' = { resp, reader ->
                    return reader?.text //Otherwise it will return a closed InputStream
                }
            }
            //FIXME: this method of setting cookies is vendor-specific. Should be done in more universal way
            response.'202' = { resp ->
                resp.getHeaders(HttpHeaders.SET_COOKIE).each {
                    String cookie = it.value.split(';')[0]
                    log.info('Adding cookie to collection: {}',cookie)
                    cookies.add(cookie)
                    return true
                }
            }          
            // handler for any failure status code:
            response.failure = { resp,reader ->
                log.error('ApiConnector: Unexpected error! while performing request: {} : {}',
                    resp.statusLine?.statusCode,
                    resp.statusLine?.reasonPhrase)

                String errorBody
                if (reader instanceof Map) {
                    errorBody = reader?.toString()
                } else if (reader instanceof String) {
                    errorBody = reader
                } else if (reader instanceof List) {
                    errorBody = reader?.toString()
                } else {
                    errorBody = reader?.text
                }
                log.error('Error body: {}', errorBody)
                if (failureCallback) {
                    failureCallback.call(resp, errorBody)
                }

                null
            }
        }
    }
    /*
        Low-level method for sending any HttpEntity
    */
    def sendEntity(Method method, String path, HttpEntity entity, Map<String, Serializable> params, Closure failureCallback = null) {
        httpBuilder.request(method, ContentType.ANY) { request ->
            uri.path = path
            uri.query = params
            request.entity = entity
            this.defaultHeaders.each{key,value ->
                headers[key] = value
            }
            headers[HttpHeaders.COOKIE] = cookies.join(';')          
            response.success = { resp ->
                //should kill response to prevent null parser bugs
                //log.info(resp.statusLine)
            }
            // handler for any failure status code:
            response.failure = { resp, reader ->
                log.error('ApiConnector: Unexpected error! while performing request: {} : {}',
                    resp.statusLine?.statusCode,
                    resp.statusLine?.reasonPhrase)

                String errorBody
                if (reader instanceof Map) {
                    errorBody = reader?.toString()
                } else {
                    errorBody = reader?.text
                }
                log.error('Error body: {}', errorBody)

                if (failureCallback) {
                    failureCallback.call(resp, errorBody)
                }

                return null
            }
        }
    }
    /*
        Shortcut method for getting data with url & params
        Similar as calling request with Method.GET and null as requestBody
    */
    def getData(ContentType contentType, String path, Map<String, Serializable> params, Closure failureCallback = null){
        return request(Method.GET, contentType, path, null, params, failureCallback)
    }
    /*
        Shortcut method for sending body consisting of file(s)
        @param path Path to send
        @param params URL parameters
        @param files Map of part name - file ref to send
    */
    def sendFiles(String path, Map<String, Serializable> params, Map<String, File> files, Closure failureCallback = null){
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
        if (files) {
            FileNameMap fileNameMap = URLConnection.fileNameMap
            files.each{ name, file ->
                String mimeType = fileNameMap.getContentTypeFor(file.name)
                mimeType = mimeType ?: ContentType.BINARY
                org.apache.http.entity.ContentType sendType =
                    org.apache.http.entity.ContentType.create(mimeType).withCharset('UTF-8')
                builder.addBinaryBody(name, file, sendType, file.name)
            }
        }
        HttpEntity entity = builder.build()        
        return sendEntity(Method.POST, path, entity, params, failureCallback)
    }

}
