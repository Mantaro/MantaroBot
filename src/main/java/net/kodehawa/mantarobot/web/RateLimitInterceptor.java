package net.kodehawa.mantarobot.web;

import br.com.brjdevs.java.utils.trove.async.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;

@Service
public class RateLimitInterceptor extends HandlerInterceptorAdapter {
	private static final RateLimiter rateLimiter = new RateLimiter(10, 60000);

	private static <T> boolean anyMatch(Enumeration<T> e, Predicate<T> p) {
		while (e.hasMoreElements()) if (p.test(e.nextElement())) return true;
		return false;
	}

	private static <T> List<T> toList(Enumeration<T> e) {
		List<T> list = new ArrayList<>();
		while (e.hasMoreElements()) list.add(e.nextElement());
		return list;
	}

	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (!rateLimiter.process(request.getRemoteAddr())) {
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			try (PrintWriter out = response.getWriter()) {
				boolean html = anyMatch(request.getHeaders("Accept"), v -> v.contains("text/html"));
				if (html) out.println(
					"<!DOCTYPE html><html><head><title>You are being Rate-limited</title></head><body><h1>"
				);
				out.println("You are being Rate-limited");
				if (html) out.println("</h1></body></html>");
			}
			return false;
		}

		return true;
	}
}