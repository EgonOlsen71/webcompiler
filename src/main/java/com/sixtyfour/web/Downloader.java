package com.sixtyfour.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to download a compiled file. This can either be the complete file or,
 * if size and part are given, just a chunk of it.
 * 
 * @author EgonOlsen
 */
@WebServlet(name = "Download", urlPatterns = { "/Download" }, initParams = {
		@WebInitParam(name = "uploadpath", value = "/uploaddata/") })
public class Downloader extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public Downloader() {
		// TODO Auto-generated constructor stub
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ServletOutputStream os = response.getOutputStream();
		String file = request.getParameter("file");
		if (file.contains("..") || file.contains("\\") || file.startsWith("/")) {
			Logger.log("Invalid file name: " + file);
			os.print("Invalid file name!");
			return;
		}

		if (!file.endsWith(".bin") && !file.endsWith(".prg") && !file.endsWith(".zip")) {
			Logger.log("Invalid file name: " + file);
			os.print("Invalid file name!");
			return;
		}

		Logger.log("Downloading " + file);
		ServletConfig sc = getServletConfig();
		String path = sc.getInitParameter("uploadpath");

		File bin = new File(path + file);
		byte[] buffer = new byte[65535];

		boolean delete = true;
		int len = 0;
		try (FileInputStream fis = new FileInputStream(bin)) {
			String part = request.getParameter("part");
			if (part != null) {
				response.setContentType("text/plain");
				// Transfer part..."part"
				String size = request.getParameter("size");
				int sizei = Integer.parseInt(size);
				int parti = Integer.parseInt(part);

				int offset = sizei * parti;
				fis.skip(offset);
				if (fis.available() > sizei) {
					os.write(++parti);
					delete = false;
				} else {
					os.write(0);
				}
				int total = 1;
				while ((len = fis.read(buffer, 0, sizei)) > -1 && total < sizei) {
					os.write(buffer, 0, len);
					total += len;
				}
				Logger.log("Bytes send: " + total);
			} else {
				response.setContentType("application/octet-stream");
				response.setHeader("Content-disposition",
						"attachment; filename=" + file.substring(file.indexOf("/") + 1));
				// Transfer whole file...
				while ((len = fis.read(buffer)) > -1) {
					os.write(buffer, 0, len);
				}
			}
		} catch (Exception e) {
			Logger.log("Failed to transfer file: " + file, e);
		} finally {
			if (delete) {
				delete(bin);
			}
		}
		// os.flush();
		Logger.log("Download finished!");
	}

	private void delete(File bin) {
		Logger.log("Deleting file: " + bin);
		boolean ok1 = bin.delete();
		boolean ok2 = bin.getParentFile().delete();
		Logger.log("Status: " + ok1 + "/" + ok2);
	}
}