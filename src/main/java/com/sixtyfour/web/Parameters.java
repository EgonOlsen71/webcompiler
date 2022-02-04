package com.sixtyfour.web;

import java.util.ArrayList;
import java.util.List;

import com.sixtyfour.cbmnative.mos6502.util.MemoryHole;

/**
 * Parameters for the compilation.
 * 
 * @author EgonOlsen
 *
 */
public class Parameters {

	private String platform;

	private String memConfig;

	private String sourceProcessing;

	private int progStart;

	private int varStart;

	private int varEnd;

	private int runtimeStart;

	private int compactLevel;

	private boolean bigRam;

	private boolean multiPart;

	private boolean retainLoops;
	
	private boolean memoryHolesValid=true;

	private List<MemoryHole> memoryHoles = new ArrayList<>();

	public void addMemoryHole(MemoryHole hole) {
		if (hole.getStartAddress() >= 0 && hole.getEndAddress() >= 0) {
			memoryHoles.add(hole);
		}
	}
	
	public List<MemoryHole> getMemoryHoles() {
		return memoryHoles;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getMemConfig() {
		return memConfig;
	}

	public void setMemConfig(String memConfig) {
		this.memConfig = memConfig;
	}

	public int getProgStart() {
		return progStart;
	}

	public void setProgStart(int progStart) {
		this.progStart = progStart;
	}

	public int getVarStart() {
		return varStart;
	}

	public void setVarStart(int varStart) {
		this.varStart = varStart;
	}

	public int getVarEnd() {
		return varEnd;
	}

	public void setVarEnd(int varEnd) {
		this.varEnd = varEnd;
	}

	public int getRuntimeStart() {
		return runtimeStart;
	}

	public void setRuntimeStart(int runtimeStart) {
		this.runtimeStart = runtimeStart;
	}

	public boolean isBigRam() {
		return bigRam;
	}

	public void setBigRam(boolean bigRam) {
		this.bigRam = bigRam;
	}

	public boolean isMultiPart() {
		return multiPart;
	}

	public void setMultiPart(boolean multiPart) {
		this.multiPart = multiPart;
	}

	public boolean isValid() {
		return progStart != -999 && varStart != -999 && varEnd != -999 && runtimeStart != -999 && this.memoryHolesValid;
	}

	public String toString() {
		return platform + "/" + memConfig + "/" + progStart + "/" + varStart + "/" + varEnd + "/" + runtimeStart + "/"
				+ bigRam + "/" + multiPart + "/" + retainLoops + "/" + sourceProcessing + "/" + compactLevel;
	}

	public boolean isRetainLoops() {
		return retainLoops;
	}

	public void setRetainLoops(boolean retainLoops) {
		this.retainLoops = retainLoops;
	}

	public String getSourceProcessing() {
		return sourceProcessing;
	}

	public void setSourceProcessing(String sourceProcessing) {
		this.sourceProcessing = sourceProcessing;
	}

	public int getCompactLevel() {
		return compactLevel;
	}

	public void setCompactLevel(int compactLevel) {
		this.compactLevel = compactLevel;
	}

	public boolean isMemoryHolesValid() {
		return memoryHolesValid;
	}

	public void setMemoryHolesValid(boolean memoryHolesValid) {
		this.memoryHolesValid = memoryHolesValid;
	}

}
