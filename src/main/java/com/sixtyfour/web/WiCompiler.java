package com.sixtyfour.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sixtyfour.Assembler;
import com.sixtyfour.Basic;
import com.sixtyfour.Loader;
import com.sixtyfour.cbmnative.NativeCompiler;
import com.sixtyfour.cbmnative.PlatformProvider;
import com.sixtyfour.cbmnative.mos6502.c64.Platform64;
import com.sixtyfour.cbmnative.mos6502.util.MemoryHole;
import com.sixtyfour.cbmnative.mos6502.util.SourcePart;
import com.sixtyfour.cbmnative.mos6502.util.SourceProcessor;
import com.sixtyfour.compression.Compressor;
import com.sixtyfour.config.CompilerConfig;
import com.sixtyfour.config.LoopMode;
import com.sixtyfour.config.MemoryConfig;
import com.sixtyfour.parser.Preprocessor;
import com.sixtyfour.parser.cbmnative.UnTokenizer;
import com.sixtyfour.system.FileWriter;
import com.sixtyfour.system.Program;
import com.sixtyfour.system.ProgramPart;

/**
 * 
 * @author EgonOlsen
 */
@WebServlet(name = "WiCompile", urlPatterns = { "/WiCompile" }, initParams = {
		@WebInitParam(name = "uploadpath", value = "/uploaddata/") })
public class WiCompiler extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public WiCompiler() {
		// TODO Auto-generated constructor stub
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/plain");
		ServletOutputStream os = response.getOutputStream();
		try {
			Parameters params = readParameters(request);
			Logger.log(params.toString());
			ServletConfig sc = getServletConfig();
			String path = sc.getInitParameter("uploadpath");
			String file = request.getParameter("file");
			String poll = request.getParameter("poll");

			if (poll != null && !poll.isBlank()) {
				poll(os, path, file);
				return;
			} else {
				new Thread() {
					@Override
					public void run() {
						Logger.log("Starting delayed compilation...");
						delayedCompilation(params, sc, path, file);
					}
				}.start();
			}

		} catch (Exception e) {
			os.print("Error: " + e.getMessage());
			Logger.log("Error!", e);
		}
		os.print("ok");
		os.flush();
	}

	private void poll(ServletOutputStream os, String path, String file) throws IOException {
		File ready = new File(path, file + ".rdy");
		if (ready.isFile()) {
			Logger.log("Compiled file for " + file + " is ready!");
			try (InputStream is = new FileInputStream(ready)) {
				String response = new String(Loader.loadBlob(is), "ISO-8859-1").replace("\n", "\r");
				Logger.log("Link to file: ["+response+"]");
				os.print(response);
			} finally {
				ready.delete();
			}
		} else {
			Logger.log("Compilation for " + file + " in progress...");
			os.print("no");
		}

	}

	private void delayedCompilation(Parameters params, ServletConfig sc, String path, String file) {

		File progress = new File(path, file + ".tmp");
		progress.delete();

		Logger.log(params.toString());
		Logger.log("Created temp file: " + progress);

		PrintWriter os;
		try {
			os = new PrintWriter(new FileOutputStream(progress));
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}

		try {
			if (file == null || file.isBlank()) {
				os.print("Error: No file name!? ");
				return;
			}
			List<String> res = new ArrayList<>();
			boolean ok = false;

			if (!params.isValid()) {
				os.print("Invalid memory address!");
			} else {
				ok = compile(params, path, file, os, res);
			}
			delete(path, file, ok ? null : res);

			if (res.size() > 0 && ok) {
				String name = res.get(0);
				if (name != null) {
					name = name.replace(path, "");
					os.print(URLEncoder.encode(name, "UTF-8"));
				} else {
					os.print("Error: No name found!? ");
				}
			} else {
				os.print("Error compiling file!");
			}
		} catch (Exception e) {
			Logger.log("Error while compiling", e);
			os.print("Error: " + e.getMessage());
		} finally {
			if (os != null) {
				os.close();
				File ready = new File(path, file + ".rdy");
				progress.renameTo(ready);
				Logger.log("Renamed temp file to " + ready);
			}
		}
	}

	private Parameters readParameters(HttpServletRequest request) {
		Parameters params = new Parameters();

		// Not transmitted ATM
		params.setVarStart(getMemoryAddress("varstart", request));
		params.setVarEnd(getMemoryAddress("varend", request));
		params.setRuntimeStart(getMemoryAddress("runtimestart", request));
		params.setBigRam(getBoolean("bigram", request));
		params.setRetainLoops(getBoolean("loops", request));
		params.setSourceProcessing(request.getParameter("source"));
		
		// Potentially transmitted
		params.setProgStart(getMemoryAddress("sa", request));
		params.setCompactLevel(getNumber(request.getParameter("cl")));
		
		// Hack, if somebody tries to compile the program up to 49152 or something...
		if (params.getProgStart()>40960) {
			Logger.log("Adjusting string memory end to 53248!");
			params.setVarEnd(53248);
		}

		String memHole = request.getParameter("mh");
		if (memHole!=null && !memHole.isBlank()) {
			String[] parts=memHole.split("-");
			if (parts.length!=2) {
				params.setMemoryHolesValid(false);
			} else {
				params.addMemoryHole(new MemoryHole(getNumber(parts[0]), getNumber(parts[1])));
			}
			
		}
		return params;
	}

	private boolean getBoolean(String parameter, HttpServletRequest request) {
		String val = request.getParameter(parameter);
		if (val != null && val.equals("1")) {
			return true;
		}
		return false;
	}

	private int getMemoryAddress(String parameter, HttpServletRequest request) {
		if (request.getParameter(parameter) == null || request.getParameter(parameter).equals("0")) {
			return -1;
		} else {
			int num = getNumber(request.getParameter(parameter));
			if (num < -1 || num > 65536) {
				return -999;
			}
			return num;
		}
	}

	private static int getNumber(String nums) {
		if (nums != null) {
			nums = nums.trim();
			if (nums.isEmpty()) {
				return -1;
			}
			try {
				if (nums.startsWith("$")) {
					nums = nums.substring(1);
					return Integer.parseInt(nums, 16);
				}
				return Integer.parseInt(nums);
			} catch (Exception e) {
				return -999;
			}
		}
		return -1;
	}

	private void delete(String path, String file, List<String> res) throws IOException {
		File fi = new File(path + file);
		boolean ok = fi.delete();
		if (!ok) {
			fi.deleteOnExit();
		}

		if (res != null) {
			for (String filey : res) {
				File fiy = new File(filey);
				fiy.delete();
				fiy.getParentFile().delete();
			}
		}
	}

	private boolean compile(Parameters params, String path, String file, PrintWriter os, List<String> res)
			throws IOException {

		if (file.contains("..") || file.contains("\\") || file.contains("/")) {
			os.print("Invalid file name: " + file);
			return false;
		}

		String blobFile = path + file;
		CompilerConfig cfg = new CompilerConfig();
		MemoryConfig memConfig = new MemoryConfig();

		cfg.setLoopMode(params.isRetainLoops() ? LoopMode.EXECUTE : LoopMode.REMOVE);
		cfg.setBigRam(params.isBigRam());
		cfg.setCompactThreshold(params.getCompactLevel());

		memConfig.setProgramStart(params.getProgStart());
		memConfig.setRuntimeStart(params.getRuntimeStart());
		memConfig.setVariableStart(params.getVarStart());
		memConfig.setStringEnd(params.getVarEnd());
		memConfig.setBasicBufferStart(0);

		PlatformProvider platform = createPlatform(cfg, params);
		configureBigram(cfg, memConfig, platform);

		String[] src = null;
		byte[] data = Loader.loadBlob(blobFile);
		UnTokenizer unto = new UnTokenizer();
		List<String> srcList = unto.getText(data, false);
		src = srcList.toArray(new String[0]);

		if (src == null || src.length == 0) {
			os.print("Source file is empty!");
			return false;
		}

		src = Preprocessor.convertSpecialChars(src);

		Basic basic = new Basic(src);
		try {
			out("Checking source file...");
			basic.compile(cfg);
		} catch (Exception e) {
			os.println("Error: " + e.getMessage());
			return false;
		}

		List<String> nCode = null;
		NativeCompiler nComp = NativeCompiler.getCompiler();

		try {
			nCode = nComp.compile(cfg, basic, memConfig, platform);
		} catch (Exception e) {
			String ll = nComp.getLastProcessedLine();
			os.print("Error: " + e.getMessage() + (ll != null ? ("-" + ll) : ""));
			return false;
		}

		nCode = processSourceCode(params, cfg, platform, nCode);

		Assembler assy = null;
		assy = new Assembler(nCode);
		try {
			assy.compile(cfg);
		} catch (Exception e) {
			os.println("Error: " + e.getMessage());
			return false;
		}

		String targetDir = path + UUID.randomUUID() + "/";
		new File(targetDir).mkdirs();
		file = targetDir + file;

		return writeTargetFiles(res, memConfig, file, nCode, assy, platform, true, false, os);

	}

	private List<String> processSourceCode(Parameters params, CompilerConfig cfg, PlatformProvider platform,
			List<String> nCode) {
		if (!params.getMemoryHoles().isEmpty()) {
			SourceProcessor srcProc = new SourceProcessor(nCode);
			List<SourcePart> parts = srcProc.split();
			nCode = srcProc.relocate(cfg, parts, params.getMemoryHoles());
		}

		if (cfg.isBigRam()) {
			SourceProcessor srcProc = new SourceProcessor(nCode);
			nCode = srcProc.moveRuntime();
		}
		return nCode;
	}

	private void configureBigram(CompilerConfig cfg, MemoryConfig memConfig, PlatformProvider platform) {
		if (platform.supportsBigRam()) {
			if (cfg.isBigRam()) {
				if (memConfig.getStringEnd() == -1) {
					memConfig.setStringEnd(53247);
				}
				out("BigRam option enabled, highest memory address available is " + memConfig.getStringEnd());
			}
		} else {
			cfg.setBigRam(false);
		}
	}

	private PlatformProvider createPlatform(CompilerConfig cfg, Parameters params) {
		return new Platform64();
	}

	private boolean writeTargetFiles(List<String> res, MemoryConfig memConfig, String targetFile, List<String> ncode,
			Assembler assy, PlatformProvider platform, boolean addrHeader, boolean compress, PrintWriter os)
			throws IOException {
		boolean ok = write6502(res, memConfig, targetFile, assy, platform, addrHeader, compress);
		// Check out of memory on write time
		int se = memConfig.getStringEnd();
		if (se <= 0) {
			se = platform.getBasicMemoryEndAddress();
		}
		if (se >= 0) {
			ProgramPart part0 = assy.getProgram().getParts().get(0);
			if (part0.getAddress() <= se && part0.getEndAddress() > se) {
				os.print("OOM: " + (part0.getEndAddress() + ">" + se));
			}
		}
		return ok;
	}

	private boolean write6502(List<String> res, MemoryConfig memConfig, String targetFile, Assembler assy,
			PlatformProvider platform, boolean addrHeader, boolean compress) {
		try {
			res.add(targetFile);
			out("Writing target file: " + targetFile);
			boolean basicHeader = memConfig.getProgramStart() == -1
					|| (memConfig.getProgramStart() < platform.getMaxHeaderAddress()
							&& memConfig.getProgramStart() >= platform.getBaseAddress() + 23);
			FileWriter.writeAsPrg(assy.getProgram(), targetFile, basicHeader, platform.getBaseAddress(), addrHeader);
			if (compress) {
				byte[] bytes = Compressor.loadProgram(targetFile);
				Program compressed = Compressor.compressAndLinkNative(bytes,
						basicHeader ? -1 : memConfig.getProgramStart());
				if (compressed != null) {
					String resultFile = targetFile.replace(".prg", "-c.prg");
					out("Writing compressed target file: " + resultFile);
					FileWriter.writeAsPrg(compressed, resultFile, false);
				} else {
					out("Unable to compress the program any further, no compressed version has been created!");
				}
			}
			return true;
		} catch (Exception e) {
			out("Failed to write target file '" + targetFile + "': " + e.getMessage());
			return false;
		}
	}

	private void out(String text) {
		Logger.log(text);
	}

}
