/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.test.filereaders;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class FileReader {
	public List<String> readFromFile(String filename) throws IOException 
	{
		
		System.out.println("Reading stream from " + filename);

		List<String> lines = new LinkedList<String>();

		InputStream stream = null;
		if(new File(filename).exists()) {
			stream = new FileInputStream(filename);
		}
		else {
			stream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(filename);
		}
		DataInputStream in = new DataInputStream(stream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		while ((strLine = br.readLine()) != null) 
		{
			//System.out.println("-----------------I READ: " + strLine);
			lines.add(strLine);
		}
		//br.close();

		return lines;

	}
}
