package com.sixtyfour.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sixtyfour.templating.Template;
import com.sixtyfour.templating.TemplateManager;

/**
 * 
 * @author EgonOlsen
 *
 */
@WebServlet(name = "Render", urlPatterns = { "/Render/*" })
public class Render extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private TemplateManager tmplMan = TemplateManager.getInstance();
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String path = request.getPathInfo();

		String template = "asm_beer.cbm";
		if (path != null && path.contains("basic")) {
			template = "basic_beer.cbm";
		}
		
		
		Template tmpl=tmplMan.getTemplate(getServletContext().getRealPath("/WEB-INF/views/"+template));
		tmpl.setVariable("mx%", 99);
		String html = tmpl.process();

		response.setContentType("text/html");
		response.getWriter().print(html);
	}

}
