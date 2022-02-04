package com.sixtyfour.web;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Uploadservlet to be used by MOSCloud to upload a file in parts via GET
 * parameters.
 * 
 * 
 * @author EgonOlsen
 */
@WebServlet(name = "ChunkedUpload", urlPatterns = { "/ChunkedUpload" }, initParams = {
		@WebInitParam(name = "uploadpath", value = "/uploaddata/") })
@MultipartConfig
public class ChunkedUploader extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ChunkedUploader() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		try {
			ServletConfig sc = getServletConfig();
			String path = sc.getInitParameter("uploadpath");
			new File(path).mkdirs();

			String fileName = request.getParameter("file");
			String data = request.getParameter("data");
			if (data != null && data.length() > 0) {
				Logger.log("Uploading chunk: " + fileName);
				if (fileName == null || fileName.isBlank()) {
					response.getOutputStream().print("Error: No file name!? ");
					return;
				}
				checkFile(fileName);

				fileName = fileName.replace(" ", "-");
				Path target = Paths.get(path + fileName);
				Logger.log("Appending data to: " + target);

				if (target.toFile().length() > 63335) {
					Logger.log("File too large, aborting...");
					target.toFile().delete();
					response.getOutputStream().print("Error: File too large");
					return;
				}

				try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(target.toFile(), true))) {
					for (int i = 0; i < data.length(); i += 2) {
						String part = data.substring(i, i + 2).toUpperCase();
						int byty = Integer.parseInt(part, 16);
						fos.write(byty);
					}
				}
				Logger.log("Chunked upload ok: " + fileName);
				response.getOutputStream().print("ok");
			} else {
				Logger.log("Nothing to upload for: " + fileName);
				response.getOutputStream().print("Error: Empty file");
			}
		} catch (Exception e) {
			Logger.log("Chunked upload failed!", e);
			response.getOutputStream().print("Error: " + e.getMessage());
		}
	}

	private void checkFile(String fileName) throws IOException {
		String fileLow = fileName.toLowerCase(Locale.ENGLISH);
		if (fileName.contains("\\") || fileName.contains("/") || fileName.contains("..") || !fileLow.endsWith(".bin")) {
			Logger.log("Invalid file name: " + fileName);
			throw new IOException("Invalid file name: " + fileName);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}
}
