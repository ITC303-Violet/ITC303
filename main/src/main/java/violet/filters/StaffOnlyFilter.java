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

public class StaffOnlyFilter implements Filter {
//	private FilterConfig config;
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpSession session = ((HttpServletRequest)request).getSession(false);
		UserBean userBean = (session != null) ? (UserBean)session.getAttribute("userBean") : null;
		
		if(userBean != null) {
			User user = userBean.getUser();
			if(user != null && user.getIsStaff()) {
				chain.doFilter(request, response);
				return;
			}
		}
		
		((HttpServletResponse)response).sendError(403);
	}
	
	public void init(FilterConfig config) throws ServletException {
//		this.config = config;
	}
	
	public void destroy() {
//		config = null;
	}
}
