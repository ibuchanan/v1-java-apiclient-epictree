package com.versionone.epictree;

import java.util.ArrayList;
import java.util.List;

public class Epic {

	public String oid;
	public String number;
	public String name;
	public String parent;
	public String pathname;
	public List<Epic> children;
	
	public Epic() {
		children = new ArrayList<Epic>();
	}
}
