package com.sixtyfour.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import com.sixtyfour.cbmnative.ProgressListener;
import com.sixtyfour.cbmnative.mos6502.c64.Platform64;
import com.sixtyfour.cbmnative.mos6502.util.MemoryHole;
import com.sixtyfour.cbmnative.mos6502.util.SourcePart;
import com.sixtyfour.cbmnative.mos6502.util.SourceProcessor;
import com.sixtyfour.cbmnative.mos6502.vic20.Platform20;
import com.sixtyfour.cbmnative.mos6502.x16.PlatformX16;
import com.sixtyfour.compression.Compressor;
import com.sixtyfour.config.CompilerConfig;
import com.sixtyfour.config.LoopMode;
import com.sixtyfour.config.MemoryConfig;
import com.sixtyfour.extensions.x16.X16Extensions;
import com.sixtyfour.parser.Preprocessor;
import com.sixtyfour.parser.cbmnative.UnTokenizer;
import com.sixtyfour.system.FileWriter;
import com.sixtyfour.system.Program;
import com.sixtyfour.system.ProgramPart;

/**
 * Servlet called by the web application to compile one or several BASIC programs. 
 * 
 * @author EgonOlsen
 */
@WebServlet(name = "Compile", urlPatterns = { "/Compile" }, initParams = {
		@WebInitParam(name = "uploadpath", value = "/uploaddata/") })
public class Compiler extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public Compiler() {
		// TODO Auto-generated constructor stub
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Logger.log("Compiler - POST called!");
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Logger.log("Compiler - GET called (or redirected from POST)!");
		Parameters params = readParameters(request);
		Logger.log(params.toString());

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/plain");

		ServletOutputStream os = response.getOutputStream();
		ServletConfig sc = getServletConfig();
		String path = sc.getInitParameter("uploadpath");

		String[] files = request.getParameterValues("filelist");
		if (files==null || files.length==0) {
			files = request.getParameterValues("filelist[]");
		}
		List<String> res = new ArrayList<>();
		boolean ok = false;

		if (!params.isValid()) {
			os.println("Invalid memory address provided, check configuration!");
		} else {
			ok = compile(params, path, files, os, res);
		}
		delete(path, files, ok ? null : res, os);

		if (res.size() > 0 && ok) {
			String name = res.get(0);
			if (res.size() > 1) {
				name = zipFiles(res, os);
			}
			setMarking(os);
			if (name != null) {
				name = name.replace(path, "");
				os.println(URLEncoder.encode(name, "UTF-8"));
			} else {
				setMarking(os);
				os.println("error");
			}
		} else {
			setMarking(os);
			os.println("error");
		}
	}

	private Parameters readParameters(HttpServletRequest request) {
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Logger.log("Got parameter '"+name+"' with value '"+request.getParameter(name)+"'!");
		}
		
		Parameters params = new Parameters();

		params.setPlatform(request.getParameter("platform"));
		params.setMemConfig(request.getParameter("memconfig"));

		params.setProgStart(getMemoryAddress("progstart", request));
		params.setVarStart(getMemoryAddress("varstart", request));
		params.setVarEnd(getMemoryAddress("varend", request));
		params.setRuntimeStart(getMemoryAddress("runtimestart", request));

		params.setBigRam(getBoolean("bigram", request));
		params.setMultiPart(getBoolean("multipart", request));
		params.setRetainLoops(getBoolean("loops", request));

		params.setSourceProcessing(request.getParameter("source"));

		params.setCompactLevel(getNumber(request.getParameter("compactlevel")));

		String[] starts = request.getParameterValues("memholestart");
		String[] ends = request.getParameterValues("memholeend");
		
		if (starts==null || starts.length==0) {
			starts = request.getParameterValues("memholestart[]");
			ends = request.getParameterValues("memholeend[]");
		}

		if (starts != null && ends != null) {
			if (starts.length == ends.length) {
				for (int i=0; i<starts.length; i++) {
					params.addMemoryHole(new MemoryHole(getNumber(starts[i]), getNumber(ends[i])));
				}
			} else {
				params.setMemoryHolesValid(false);
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
		if (request.getParameter(parameter + "default").equals("1")) {
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

	private void setMarking(ServletOutputStream os) throws IOException {
		os.println("~~~~");
	}

	private String zipFiles(List<String> res, ServletOutputStream os) throws IOException {
		String dir = res.get(0).substring(0, res.get(0).lastIndexOf("/") + 1);
		String zipName = dir + "files.zip";
		byte[] buffer = new byte[8192];

		os.println("Zipping files...");
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName))) {
			int cnt = 0;
			String lastDir = null;
			Set<String> used = new HashSet<>();
			for (String file : res) {
				out("Processing file: " + file);

				int pos = file.lastIndexOf("/");
				String entryName = file.substring(pos + 1);
				String dirName = file.substring(0, pos);
				File dirDir = new File(dirName);

				if (used.contains(entryName)) {
					pos = entryName.lastIndexOf(".");
					entryName = entryName.substring(0, pos) + "-" + cnt + entryName.substring(pos);
				}

				used.add(entryName);

				out.putNextEntry(new ZipEntry(entryName));
				try (InputStream in = new FileInputStream(file)) {
					int len;
					while ((len = in.read(buffer)) > -1) {
						out.write(buffer, 0, len);
					}
				} catch (Exception e) {
					Logger.log("Failed to create zip file!", e);
					throw new RuntimeException(e);
				} finally {
					File toDel = new File(file);
					toDel.delete();
					if (!dirName.equals(lastDir)) {
						cnt++;
						lastDir = dirName;
					}
					if (cnt > 1 && dirDir.listFiles().length == 0) {
						toDel.getParentFile().delete();
					}
				}
			}
		} catch (Exception e) {
			Logger.log("Failed to create zip file!", e);
			return null;
		}
		os.println(res.size() + " files zipped!");
		return zipName;

	}

	private void delete(String path, String[] files, List<String> res, ServletOutputStream os) throws IOException {
		os.println("Deleting source files...");
		for (String file : files) {
			File fi = new File(path + file);
			boolean ok = fi.delete();
			if (!ok) {
				fi.deleteOnExit();
			}
		}

		if (res != null) {
			os.println("Deleting target files...");
			for (String file : res) {
				File fi = new File(file);
				fi.delete();
				fi.getParentFile().delete();
			}
		}

		os.flush();
	}

	private boolean compile(Parameters params, String path, String[] files, ServletOutputStream os, List<String> res) {

		PrintStream ps = new PrintStream(new OutputStream() {
			private int cnt = 0;

			@Override
			public void write(int val) throws IOException {
				os.write(val);
				cnt++;
				if (cnt >= 32 || (char) val=='\n') {
					os.flush();
					cnt = 0;
				}
			}
		});
		com.sixtyfour.Logger.setThreadBoundPrintStream(ps);

		try {
			for (int i=0; i<40; i++) {
				// Force output...
				os.println("---------------------------------------");
			}
			os.flush();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for (String file : files) {

			if (file.contains("..") || file.contains("\\") || file.contains("/")) {
				out("Invalid file name: " + file);
				return false;
			}

			String srcFile = file.substring(file.indexOf("_") + 1);
			out("Compiling " + srcFile);
			file = path + file;

			long s = System.currentTimeMillis();
			CompilerConfig cfg = new CompilerConfig();
			MemoryConfig memConfig = new MemoryConfig();

			cfg.setConvertStringToLower(params.getSourceProcessing().equalsIgnoreCase("tolower"));
			cfg.setFlipCasing(params.getSourceProcessing().equalsIgnoreCase("flipcase"));
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

			String appendix = ".prg";
			boolean multiByteTokens = params.getPlatform().equalsIgnoreCase("x16");

			cfg.setProgressListener(new ProgressListener() {

				int cnt = 0;

				@Override
				public synchronized void nextStep() {
					ps.print("*");
					if (cnt++ >= 40) {
						ps.println();
						cnt = 0;
					}
				}

				@Override
				public void start() {
					//
				}

				@Override
				public void done() {
					ps.println();
				}

			});

			String[] src = null;

			if (srcFile.toLowerCase(Locale.ENGLISH).endsWith(".prg")) {
				try {
					out("Looks like a PRG file, trying to convert it...");
					byte[] data = Loader.loadBlob(file);
					UnTokenizer unto = new UnTokenizer();
					List<String> srcList = unto.getText(data, multiByteTokens);
					src = srcList.toArray(new String[0]);
					out("PRG file converted into ASCII, proceeding!");
					srcFile = srcFile.replace(".prg", ".bas");
				} catch (Exception e) {
					out("Failed to convert PRG file: " + e.getClass() + "/" + e.getMessage());
					out("Proceeding as if it was ASCII instead!");
				}
			}

			if (src == null) {
				src = loadSource(file);
			}

			if (src == null || src.length == 0) {
				System.out.println("\nSource file is empty!");
				return false;
			}

			for (String line : src) {
				if (!line.trim().isEmpty()) {
					char c = line.charAt(0);
					if (!Character.isDigit(c)) {
						out("Code seems to use labels, not lines...converting it!");
						src = Preprocessor.convertToLineNumbers(src);
					}
					break;
				}
			}

			src = Preprocessor.convertSpecialChars(src);

			Basic basic = new Basic(src);
			try {
				out("Checking source file...");
				basic.compile(cfg);
			} catch (Exception e) {
				out("\n!!! Error compiling BASIC program: " + e.getMessage());
				printCause(e);
				return false;
			}

			List<String> nCode = null;
			NativeCompiler nComp = NativeCompiler.getCompiler();

			try {
				nCode = nComp.compile(cfg, basic, memConfig, platform);
			} catch (Exception e) {
				out("\n!!! Error compiling: " + e.getMessage());
				String ll=nComp.getLastProcessedLine();
				if (ll!=null) {
					out("Error at : " + ll);
				}
				printCause(e);
				return false;
			}
			
			nCode = processSourceCode(params, cfg, platform, nCode);

			Assembler assy = null;
			if (is6502Platform(platform)) {
				assy = new Assembler(nCode);
				try {
					assy.compile(cfg);
				} catch (Exception e) {
					out("\n!!! Error running assembler: " + e.getMessage());
					printCause(e);
					return false;
				}
			}

			String targetFile = "++" + new File(srcFile).getName().replace(".BAS", "").replace(".bas", "")
					.replace(".prg", "").replace(".PRG", "") + appendix;
			String targetDir = path + UUID.randomUUID() + "/";
			new File(targetDir).mkdirs();
			targetFile = targetDir + targetFile;

			writeTargetFiles(res, memConfig, targetFile, nCode, assy, platform, true, params.isMultiPart(), false);

			out(srcFile + " compiled in " + (System.currentTimeMillis() - s) + "ms!\n\n");

		}
		return true;

	}

	private List<String> processSourceCode(Parameters params, CompilerConfig cfg, PlatformProvider platform,
			List<String> nCode) {
		if (is6502Platform(platform) && !params.getMemoryHoles().isEmpty()) {
			SourceProcessor srcProc = new SourceProcessor(nCode);
			List<SourcePart> parts = srcProc.split();
			nCode = srcProc.relocate(cfg, parts, params.getMemoryHoles());
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
		PlatformProvider platform = new Platform64();
		String pl = params.getPlatform();
		if (pl.equalsIgnoreCase("c64")) {
			platform = new Platform64();
		} else if (pl.equalsIgnoreCase("x16")) {
			platform = new PlatformX16();
			Basic.registerExtension(new X16Extensions());
			cfg.setNonDecimalNumbersAware(true);
			cfg.setAggressiveFloatOptimizations(false);
		} else if (pl.equalsIgnoreCase("vic20") || pl.equalsIgnoreCase("vc20")) {
			platform = new Platform20();
			String vicConf = params.getMemConfig();
			if (vicConf != null) {
				if (vicConf.equals("0")) {
					((Platform20) platform).setNewBaseAddress(4097);
					((Platform20) platform).setBasicMemoryEndAddress(7679);
				} else if (vicConf.equals("3")) {
					((Platform20) platform).setNewBaseAddress(1025);
					((Platform20) platform).setBasicMemoryEndAddress(7679);
				}
			}
		}
		return platform;
	}

	private String[] loadSource(String srcFile) {
		String[] src = null;
		out("Loading source file...");
		try {
			src = Loader.loadProgram(srcFile);
		} catch (Exception e) {
			out("Failed to load source file: " + e.getMessage());
			return null;
		}
		List<String> res = new ArrayList<>();
		for (String line : src) {
			if (!line.trim().startsWith("!")) {
				res.add(line);
			}
		}
		return res.toArray(new String[res.size()]);
	}

	private boolean is6502Platform(PlatformProvider platform) {
		return platform instanceof Platform64 || platform instanceof Platform20 || platform instanceof PlatformX16;
	}

	private boolean writeTargetFiles(List<String> res, MemoryConfig memConfig, String targetFile, List<String> ncode,
			Assembler assy, PlatformProvider platform, boolean addrHeader, boolean multiPart, boolean compress) {
		if (is6502Platform(platform)) {
			boolean ok = write6502(res, memConfig, targetFile, assy, platform, addrHeader, multiPart, compress);
			// Check out of memory on write time
			int se = memConfig.getStringEnd();
			if (se <= 0) {
				se = platform.getBasicMemoryEndAddress();
			}
			if (se >= 0) {
				ProgramPart part0 = assy.getProgram().getParts().get(0);
				if (part0.getAddress() <= se && part0.getEndAddress() > se) {
					out("\nWARNING: Compiled program's length exceeds memory limit: "
							+ (part0.getEndAddress() + ">" + se));
				}
			}
			return ok;
		}
		out("\n!!! Unsupported platform: " + platform);
		return false;
	}

	private boolean write6502(List<String> res, MemoryConfig memConfig, String targetFile, Assembler assy,
			PlatformProvider platform, boolean addrHeader, boolean multiPart, boolean compress) {
		try {
			if (!multiPart) {
				res.add(targetFile);
				out("Writing target file: " + targetFile);
				boolean basicHeader = memConfig.getProgramStart() == -1
						|| (memConfig.getProgramStart() < platform.getMaxHeaderAddress()
								&& memConfig.getProgramStart() >= platform.getBaseAddress() + 23);
				FileWriter.writeAsPrg(assy.getProgram(), targetFile, basicHeader, platform.getBaseAddress(),
						addrHeader);
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
			} else {
				out("Writing multiple target files!");
				Program tmp = new Program();
				Program sp = assy.getProgram();
				tmp.setLabelsContainer(sp.getLabelsContainer());
				tmp.addPart(sp.getParts().get(0));
				tmp.setCodeStart(sp.getCodeStart());
				out("Writing target file: " + targetFile);
				res.add(targetFile);
				FileWriter.writeAsPrg(tmp, targetFile,
						memConfig.getProgramStart() == -1
								|| (memConfig.getProgramStart() < platform.getMaxHeaderAddress()
										&& memConfig.getProgramStart() >= platform.getBaseAddress() + 23),
						platform.getBaseAddress(), addrHeader);
				String master = targetFile.replace(".prg", ".p0");
				for (int i = 1; i < sp.getParts().size(); i++) {
					tmp = new Program();
					ProgramPart pp = sp.getParts().get(i);
					tmp.addPart(pp);
					tmp.setCodeStart(pp.getAddress());
					tmp.setLabelsContainer(sp.getLabelsContainer());
					String newName = master + i;
					delete(newName);
					res.add(newName);
					out("Writing target file: " + newName);
					FileWriter.writeAsPrg(tmp, newName, false, platform.getBaseAddress(), true);
				}
			}
			return true;
		} catch (Exception e) {
			out("Failed to write target file '" + targetFile + "': " + e.getMessage());
			return true;
		}
	}

	private boolean delete(String ilTarget) {
		return !new File(ilTarget).exists() || new File(ilTarget).delete();
	}

	private void printCause(Exception e) {
		if (e.getCause() != null && e.getCause().getMessage() != null) {
			out("Caused by: " + e.getCause().getMessage());
		}
	}

	private void out(String text) {
		com.sixtyfour.Logger.log(text);
		Logger.log(text);
	}

}
