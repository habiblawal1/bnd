package aQute.bnd.http;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.converter.TypeReference;
import aQute.service.reporter.Reporter;

/**
 * Builds up a request
 * 
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class HttpRequest<T> {
	String				verb		= "GET";
	Object				upload;
	Type				download;
	Map<String,String>	headers		= new HashMap<>();
	long				timeout		= -1;
	HttpClient			client;
	String				ifNoneMatch;
	long				ifModifiedSince;
	long				ifUnmodifiedSince;
	URL					url;
	int					redirects	= 10;
	String				ifMatch;
	boolean				cached;
	long				maxStale;
	Reporter			reporter;
	File				useCacheFile;
	boolean				updateTag;

	HttpRequest(HttpClient client) {
		this.client = client;
	}

	/**
	 * Convert the result to a specific type
	 */
	public <X> HttpRequest<X> get(Class<X> type) {
		this.download = type;
		return (HttpRequest<X>) this;
	}

	/**
	 * Convert the result to a specific type
	 */
	public <X> HttpRequest<X> get(TypeReference<X> type) {
		this.download = type.getType();
		return (HttpRequest<X>) this;
	}

	/**
	 * Convert the result to a specific type
	 */
	public HttpRequest<Object> get(Type type) {
		this.download = type;
		return (HttpRequest<Object>) this;
	}

	/**
	 * Set the HTTP verb
	 */

	public HttpRequest<T> verb(String verb) {
		this.verb = verb;
		return this;
	}

	/**
	 * Set the verb/method to put
	 */
	public HttpRequest<T> put() {
		this.verb = "PUT";
		return this;
	}

	/**
	 * Set the verb/method to head
	 */
	public HttpRequest<T> head() {
		this.verb = "HEAD";
		return this;
	}

	/**
	 * Set the verb/method to get
	 */
	public HttpRequest<T> get() {
		this.verb = "GET";
		return this;
	}

	/**
	 * Set the verb/method to post
	 */
	public HttpRequest<T> post() {
		this.verb = "POST";
		return this;
	}

	/**
	 * Set the verb/method to option
	 */
	public HttpRequest<T> option() {
		this.verb = "OPTION";
		return this;
	}

	/**
	 * Set the verb/method to delete
	 */
	public HttpRequest<T> delete() {
		this.verb = "DELETE";
		return this;
	}

	/**
	 * Set the object to upload. Can be of several types:
	 * <ul>
	 * <li>InputStream – copied verbatim
	 * <li>String – content sent
	 * <li>byte[] – content sent
	 * <li>File – content sent
	 * <li>Otherwise assumes DTO and encodes in JSON
	 * </ul>
	 */
	public HttpRequest<T> upload(Object upload) {
		this.upload = upload;
		return this;
	}

	/**
	 * Add headers to request
	 */
	public HttpRequest<T> headers(Map<String,String> map) {
		headers.putAll(map);
		return this;
	}

	/**
	 * Add header to request
	 */
	public HttpRequest<T> headers(String key, String value) {
		headers.put(key, value);
		return this;
	}

	/**
	 * Set timeout in ms
	 */
	public HttpRequest<T> timeout(long timeoutInMs) {
		this.timeout = timeoutInMs;
		return this;
	}

	public HttpRequest<T> ifNoneMatch(String etag) {
		this.ifNoneMatch = etag;
		return this;
	}

	public HttpRequest<T> ifModifiedSince(long epochTime) {
		this.ifModifiedSince = epochTime;
		return this;
	}

	public HttpRequest<T> maxRedirects(int n) {
		this.redirects = n;
		return this;
	}

	public T go(URL url) throws Exception {
		this.url = url;
		return (T) client.send(this);
	}

	public T go(URI url) throws Exception {
		this.url = url.toURL();
		return (T) client.send(this);
	}

	public HttpRequest<T> age(int n, TimeUnit tu) {
		this.headers.put("Age", "" + tu.toSeconds(n));
		return this;
	}

	public Promise<T> async(final URL url) {
		this.url = url;
		final Deferred<T> deferred = new Deferred<>();
		Executor e = Processor.getExecutor();
		e.execute(new Runnable() {

			@Override
			public void run() {
				try {
					T result = (T) client.send(HttpRequest.this);
					deferred.resolve(result);
				} catch (Exception t) {
					deferred.fail(t);
				}
			}

		});
		return deferred.getPromise();
	}

	@Override
	public String toString() {
		return "HttpRequest [verb=" + verb + ", upload=" + upload + ", download=" + download + ", headers=" + headers
				+ ", timeout=" + timeout + ", client=" + client + ", url=" + url + "]";
	}

	public HttpRequest<T> ifUnmodifiedSince(long ifNotModifiedSince) {
		this.ifUnmodifiedSince = ifNotModifiedSince;
		return this;
	}

	public HttpRequest<T> ifMatch(String etag) {
		this.ifMatch = etag;
		return this;
	}

	public HttpRequest<TaggedData> asTag() {
		return get(TaggedData.class);
	}

	public HttpRequest<String> asString() {
		return get(String.class);
	}

	public boolean isCache() {
		return ("GET".equalsIgnoreCase(verb) && cached) || download == File.class;
	}

	public HttpRequest<File> useCache(long maxStale) {
		this.maxStale = maxStale;
		this.cached = true;
		download = File.class;
		return (HttpRequest<File>) this;
	}

	public HttpRequest<File> useCache() {
		return useCache(-1);
	}

	public HttpRequest<File> useCache(File file) {
		this.useCacheFile = file;
		return useCache(-1);
	}

	public HttpRequest<File> useCache(File file, long maxStale) {
		this.useCacheFile = file;
		return useCache(maxStale);
	}

	public HttpRequest<T> report(Reporter reporter) {
		this.reporter = reporter;
		return this;
	}

	// TODO
	public HttpRequest<T> timeout(long timeout, TimeUnit unit) {
		this.timeout = timeout;
		return this;
	}

	public boolean isTagResult() {
		return download == null || download == TaggedData.class;
	}

	public HttpRequest<T> updateTag() {
		updateTag = true;
		return this;
	}
}
