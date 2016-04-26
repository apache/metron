/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.enrichment.tldextractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicTldExtractor implements Serializable {
	private static final long serialVersionUID = -7440226111118873815L;
	private StringBuilder sb = new StringBuilder();

    private Pattern pattern;
    
    /**
    * The inputFile.
    */
   private String inputFile ="effective_tld_names.dat";
   
   public BasicTldExtractor(String filePath) {
       this.inputFile=filePath;
       this.init();
   }
   
	public BasicTldExtractor() {
      this.init();
	}

	private void init(){
	       try {
	            ArrayList<String> terms = new ArrayList<String>();

	            
	            BufferedReader br = new BufferedReader(new InputStreamReader(
	                    getClass().getClassLoader().getResourceAsStream(inputFile)));
	            String s = null;
	            while ((s = br.readLine()) != null) {
	                s = s.trim();
	                if (s.length() == 0 || s.startsWith("//") || s.startsWith("!"))
	                    continue;
	                terms.add(s);
	            }
	            Collections.sort(terms, new StringLengthComparator());
	            for (String t : terms)
	                add(t);
	            compile();
	            br.close();
	        } catch (IOException e) {
	            throw new IllegalStateException(e);
	        }
	}
	protected void add(String s) {
		s = s.replace(".", "\\.");
		s = "\\." + s;
		if (s.startsWith("*")) {
			s = s.replace("*", ".+");
			sb.append(s).append("|");
		} else {
			sb.append(s).append("|");
		}
	}

	public void compile() {
		if (sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);
		sb.insert(0, "[^.]+?(");
		sb.append(")$");
		pattern = Pattern.compile(sb.toString());
		sb = null;
	}

	public String extract2LD(String host) {
		Matcher m = pattern.matcher(host);
		if (m.find()) {
			return m.group(0);
		}
		return null;
	}

	public String extractTLD(String host) {
		Matcher m = pattern.matcher(host);
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

	public static class StringLengthComparator implements Comparator<String> {
		public int compare(String s1, String s2) {
			if (s1.length() > s2.length())
				return -1;
			if (s1.length() < s2.length())
				return 1;
			return 0;
		}
	}
    /**
     * Returns the sb.
     * @return the sb.
     */
    
    public StringBuilder getSb() {
        return sb;
    }

    /**
     * Sets the sb.
     * @param sb the sb.
     */
    
    public void setSb(StringBuilder sb) {
    
        this.sb = sb;
    }
    /**
     * Returns the inputFile.
     * @return the inputFile.
     */
    
    public String getInputFile() {
        return inputFile;
    }

    /**
     * Sets the inputFile.
     * @param inputFile the inputFile.
     */
    
    public void setInputFile(String inputFile) {
    
        this.inputFile = inputFile;
    }
}
