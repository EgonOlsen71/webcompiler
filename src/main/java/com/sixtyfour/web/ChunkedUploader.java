package com.sixtyfour.web;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
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
				checkFile(fileName);

				fileName = fileName.replace(" ", "-");
				Path target = Paths.get(path + fileName);
				Logger.log("Appending data to: " + target);

				try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(target.toFile(), true))) {
					//StringBuilder sb=new StringBuilder();
					for (int i = 0; i < data.length(); i += 2) {
						String part = data.substring(i, i + 2).toUpperCase();
						int byty = Integer.parseInt(part, 16);
						fos.write(byty);
						//sb.append((char)byty);
					}
					//data = sb.toString();
					//System.out.println(data);
				}
				Logger.log("Chunked upload ok: " + fileName);
				response.getOutputStream().print("ok");
			} else {
				Logger.log("Nothing to upload for: " + fileName);
				response.getOutputStream().print("error: empty file");
			}
		} catch (Exception e) {
			Logger.log("Chunked upload failed!", e);
			response.getOutputStream().print("error: "+e.getMessage());
		}
	}

	private void checkFile(String fileName) throws IOException {
		if (fileName.contains("\\") || fileName.contains("/") || fileName.contains("..")) {
			Logger.log("Invalid file name: " + fileName);
			throw new IOException("Invalid file name: " + fileName);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	private void returnError() throws IOException {
		throw new IOException("Upload failed!");
	}

}
