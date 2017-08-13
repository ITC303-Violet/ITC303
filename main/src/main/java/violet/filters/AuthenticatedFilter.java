package violet.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import violet.beans.UserBean;
import violet.jpa.User;

/**
 * Filters requests to ensure only authenticated users can access a portion of the site
 * @author somer
 */
public class AuthenticatedFilter implements Filter {
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpSession session = ((HttpServletRequest)request).getSession(false);
		UserBean userBean = (session != null) ? (UserBean)session.getAttribute("userBean") : null; // attempt to grab the userbean from the session
		
		if(userBean != null) {
			User user = userBean.getUser();
			if(user != null) { // if the user is signed in, forward request to other filters
				chain.doFilter(request, response);
				return;
			}
		}
		
		// otherwise return a 403 code
		((HttpServletResponse)response).sendError(403);
	}
	
	public void init(FilterConfig config) throws ServletException {
	}
	
	public void destroy() {
	}
}
