package org.sctx;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

import org.sctx.ContextInferenceEngine.updateQueueNode;

import android.util.SparseArray;

public class ContextInferenceEngine {
	
	static class ContextSymbol {
		int id;
		int holdCount;
		String name;
		Set<Clause> clauses;
	}
	
	static class Clause {
		ContextSymbol symbol;
		int waitingCount;
		HashMap<String, Integer> symbols;

		void notifySymbol(String sym, boolean v) {
			if (!symbols.containsKey(sym))
				return;
			int i = symbols.get(sym);
			if (i == 0 && v) {
				--waitingCount;
				symbols.put(sym, 2);
			} else if (i == 1 && !v) {
				--waitingCount;
				symbols.put(sym, 3);
			} else if (i == 2 && !v) {
				++waitingCount;
				symbols.put(sym, 0);
			} else if (i == 3 && v) {
				++waitingCount;
				symbols.put(sym, 1);
			}
		}

		boolean holds() {
			return waitingCount == 0;
		}

		public String toString() {
			StringBuilder out = new StringBuilder();
			Iterator<String> it = symbols.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				int v = symbols.get(key); 
				if ((v & 1) == 1)
					out.append("!");
				out.append(key);
				if (it.hasNext())
					out.append("+");
			}
			return out.toString();
		}
	}

	HashMap<String, Boolean> symbols;
	HashMap<String, HashSet<Clause>> clauses;
	ArrayList<ContextSymbol> symbolsById;
	
	ContextSymbol parse(String str) {
		ContextSymbol d = new ContextSymbol();
		Scanner s = new Scanner(str);
		s.useDelimiter(",");
		try {
			String name = s.next();
			// Invalid name
			if (name.contains("@")) return null;
			if (symbols.containsKey(name)) return null;
			d.holdCount = 0;
			d.name = name;
			d.clauses = new HashSet<Clause>();
			while (s.hasNext()) {
				String c_str = s.next();
				Clause c = parseClause(d, c_str);
				if (c == null) return null;
				if (c.holds()) ++ d.holdCount;
				d.clauses.add(c);
			}
		} catch (Exception x) {
			return null;
		}
		
		d.id = symbolsById.size();
		symbolsById.add(d);
		symbols.put(d.name, d.holdCount > 0);
		
		Iterator<Clause> it = d.clauses.iterator();
		while (it.hasNext()) {
			Clause c = it.next();
			Iterator<String> itemIt = c.symbols.keySet().iterator();
			while (itemIt.hasNext()) {
				String item = itemIt.next();
				if (!clauses.containsKey(item))
					clauses.put(item, new HashSet<Clause>());
				clauses.get(item).add(c);
			}
		}
		return d;
	}
	
	Clause parseClause(ContextSymbol d, String str) {
		Clause c = new Clause();
		c.symbol = d;
		c.waitingCount = 0;
		c.symbols = new HashMap<String, Integer>();
		
		Scanner s = new Scanner(str);
		s.useDelimiter("\\+");
		while (s.hasNext()) {
			String item = s.next();
			int m;
			if (item.startsWith("!")) {
				item = item.substring(1);
				m = 1;
			} else {
				m = 0;
			}

			if (!item.contains("@") && !symbols.containsKey(item)) return null;
			
			boolean value = symbols.containsKey(item) ? symbols.get(item) : false;
			if (m == 0 && value) m = 2;
			else if (m == 1 && !value) m = 3;
			else ++ c.waitingCount;
			
			c.symbols.put(item, m);
		}
		return c;
	}
	
	void init(Reader _in) {
		BufferedReader in = new BufferedReader(_in);

		symbols = new HashMap<String, Boolean>();
		clauses = new HashMap<String, HashSet<Clause>>();
		symbolsById = new ArrayList<ContextSymbol>();
		
		while (true) {
			String line;
			try {
				line = in.readLine();
			} catch (Exception x) {
				break;
			}
			if (line == null) break;
			if (parse(line) == null)
				Util.log("Error while parsing line " + line);
		}
	}
	
	void resetSymbolValues() {
		for (int i = 0; i < symbolsById.size(); ++ i) {
			ContextSymbol s = symbolsById.get(i);
			s.holdCount = 0;
			Iterator<Clause> it = s.clauses.iterator();
			while (it.hasNext()) {
				Clause c = it.next();
				c.waitingCount = c.symbols.size();
				Iterator<String> itemIt = c.symbols.keySet().iterator();
				while (itemIt.hasNext()) {
					String item = itemIt.next();
					int m = c.symbols.get(item) & 1;
					boolean f = symbols.containsKey(item) ? symbols.get(item) : false;
					if (f && m == 0) {
						m = 2;
						-- c.waitingCount;
					}
					else if (!f && m == 1) {
						m = 3;
						-- c.waitingCount;
					}
					c.symbols.put(item, m);
				}
				
				if (c.waitingCount == 0) ++ s.holdCount;
			}
			symbols.put(s.name, s.holdCount > 0);
		}
	}
	
	class updateQueueNode {
		int id;
		String name;
		
		updateQueueNode(int id, String name) {
			this.id = id;
			this.name = name;
		}
	};
	
	void updateSystemSymbol(String name, boolean value, Collection<String> modifySet) {
		if (!name.contains("@")) return;
		boolean old = symbols.containsKey(name) ? symbols.get(name) : false;
		if (old == value) return;
		symbols.put(name, value);
		
		PriorityQueue<updateQueueNode> queue = new PriorityQueue<updateQueueNode>(1, new Comparator<updateQueueNode>() {
			@Override
			public int compare(updateQueueNode lhs, updateQueueNode rhs) {
				if (lhs.id < rhs.id) return -1;
				else if (lhs.id > rhs.id) return 1;
				else return 0;
			}
		});
		// Native contexts have no ids
		queue.add(new updateQueueNode(-1,name));
		
		String last = null;
		// Update values of symbol according to the order
		while (!queue.isEmpty()) {
			updateQueueNode node = queue.poll();
			if (node.name.equals(last)) continue;
			modifySet.add(node.name);
			Util.log("!!! " + node.name);
			last = node.name;
			
			boolean v = symbols.get(node.name);
			HashSet<Clause> affectClauses = clauses.get(node.name);
			if (affectClauses == null) continue;
			
			Iterator<Clause> it = affectClauses.iterator();
			while (it.hasNext()) {
				Clause c = it.next();
				boolean h = c.holds();
				c.notifySymbol(node.name, v);
				if (h != c.holds()) {
					if (!h) {
						if (++ c.symbol.holdCount == 1) {
							symbols.put(c.symbol.name, true);
							queue.add(new updateQueueNode(c.symbol.id, c.symbol.name));
						}
					} else {
						if (-- c.symbol.holdCount == 0) {
							symbols.put(c.symbol.name, false);
							queue.add(new updateQueueNode(c.symbol.id, c.symbol.name));
						}
					}
				}
			}
		}
	}
}

