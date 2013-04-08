package org.sctx;

class util {
	public void log(String msg) {
		if (EntryActivity.singleton == null) return;
		EntryActivity.singleton.logText.append(msg);
	}
}
