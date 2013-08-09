package integration.rewrite;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.PassThroughFilterChain;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RewriteTests {

	private FilterChain filterChain;

	@Before
	public void setUp() throws Exception {
		UrlRewriteFilter filter = createUrlFilter("rewriteFilter", "urlrewrite.xml");
		UrlRewriteFilter mappingsFilter = createUrlFilter("mappingsRewriteFilter", "mappings_rewrite.xml");
		filterChain = new PassThroughFilterChain(mappingsFilter, new PassThroughFilterChain(filter, new MockFilterChain()));
	}

	private UrlRewriteFilter createUrlFilter(String filterName, String mappingsFile) throws ServletException {
		UrlRewriteFilter newFilter = new UrlRewriteFilter();
		MockFilterConfig filterConfig = new MockFilterConfig(filterName);
		filterConfig.addInitParameter("confPath", mappingsFile);
		newFilter.init(filterConfig);
		return newFilter;
	}

	@Test
	public void legacySiteRedirects() throws Exception {
		validatePermanentRedirect("http://www.springsource.org/sts/welcome", "http://springframework.io/tools/sts/welcome");
		validatePermanentRedirect("http://www.springsource.org/groovy-grails-tool-suite-download", "http://springframework.io/tools/ggts");
		validateTemporaryRedirect("http://www.springsource.org/ggts/welcome", "http://grails.org/products/ggts");
	}

	@Test
	public void blogPagesAreRedirected() throws Exception {
		validatePermanentRedirect("http://blog.springsource.org/anything", "http://springframework.io/blog/anything");
	}

	@Test
	public void blogAssetsAreRedirected() throws Exception {
		validateTemporaryRedirect("http://blog.springsource.org/wp-content/uploads/attachment.zip", "http://wp.springframework.io/wp-content/uploads/attachment.zip");
	}

	@Test
	public void blogAuthorsAreRedirected() throws Exception {
		validatePermanentRedirect("http://blog.springsource.org/author/cbeams", "http://springframework.io/team/cbeams");
	}

	@Test
	public void drupalNodesAreRedirected() throws Exception {
		validatePermanentRedirect("http://blog.springsource.org/BusinessIntelligenceWithSpringAndBIRT", "http://springframework.io/blog/2012/01/30/spring-framework-birt");
	}

	@Test
	public void oldCaseStudiesAreRedirected() throws Exception {
		validateTemporaryRedirect("http://www.springsource.org/files/uploads/file.pdf", "http://drupal.springframework.io/files/uploads/file.pdf");
	}

	@Test
	public void videosRedirectToYoutube() throws ServletException, IOException, URISyntaxException {
		validateTemporaryRedirect("http://www.springsource.org/videos", "http://www.youtube.com/springsourcedev");
		validateTemporaryRedirect("http://www.example.com/videos", "http://www.youtube.com/springsourcedev");
	}
	@Test
	public void gsgGuidesShouldAlwaysHaveTrailingSlash() throws ServletException, IOException, URISyntaxException {
		validatePermanentRedirect("/guides/gs/guide-name", "/guides/gs/guide-name/");
	}

	@Test
	public void gsgGuidesListingRedirectsToIndex() throws ServletException, IOException, URISyntaxException {
		validateTemporaryRedirect("/guides/gs/", "/guides#gs");
		validateTemporaryRedirect("/guides/gs", "/guides#gs");
	}

	@Test
	public void gsgTutorialsListingRedirectsToIndex() throws ServletException, IOException, URISyntaxException {
		validateTemporaryRedirect("/guides/tutorials/", "/guides#tutorials");
		validateTemporaryRedirect("/guides/tutorials", "/guides#tutorials");
	}

	@Test
	public void stripsWwwSubdomain() throws ServletException, IOException, URISyntaxException {
		validatePermanentRedirect("http://www.springsource.org/something", "http://springframework.io/something");
		validatePermanentRedirect("http://www.example.com/something", "http://springframework.io/something");
	}

	@Test
	public void projectPageIndexIsNotRedirected() throws ServletException, IOException, URISyntaxException {
		validateOk("http://springframework.io/projects");
	}

	@Test
	public void projectPageIndexWithSlashIsNotRedirected() throws ServletException, IOException, URISyntaxException {
		validateOk("http://springframework.io/projects/");
	}

	@Test
	public void projectPagesAreRedirected() throws ServletException, IOException, URISyntaxException {
		validateTemporaryRedirect("http://springframework.io/projects/spring-data", "http://projects.springframework.io/spring-data");
		validateTemporaryRedirect("http://springframework.io/projects/not-exist", "http://projects.springframework.io/not-exist");
	}

	private void validateTemporaryRedirect(String requestedUrl, String redirectedUrl) throws IOException, ServletException, URISyntaxException {
		validateRedirect(requestedUrl, redirectedUrl, 302);
	}

	private void validatePermanentRedirect(String requestedUrl, String redirectedUrl) throws IOException, ServletException, URISyntaxException {
		validateRedirect(requestedUrl, redirectedUrl, 301);
	}

	private void validateRedirect(String requestedUrl, String redirectedUrl, int expectedStatus) throws IOException, ServletException, URISyntaxException {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		URI requestUrl = new URI(requestedUrl);
		if (requestUrl.getHost() != null) {
			servletRequest.addHeader("host", requestUrl.getHost());
		}
		servletRequest.setRequestURI(requestUrl.getPath());
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		filterChain.doFilter(servletRequest, servletResponse);

		assertThat(servletResponse.getRedirectedUrl(), equalTo(redirectedUrl));
		assertThat("Incorrect status code for " + redirectedUrl, servletResponse.getStatus(), equalTo(expectedStatus));
	}

	private void validateOk(String requestedUrl) throws IOException, ServletException, URISyntaxException {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		URI requestUrl = new URI(requestedUrl);
		if (requestUrl.getHost() != null) {
			servletRequest.addHeader("host", requestUrl.getHost());
		}
		servletRequest.setRequestURI(requestUrl.getPath());
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		filterChain.doFilter(servletRequest, servletResponse);

		assertThat(servletResponse.getStatus(), equalTo(HttpStatus.OK.value()));
	}
}
