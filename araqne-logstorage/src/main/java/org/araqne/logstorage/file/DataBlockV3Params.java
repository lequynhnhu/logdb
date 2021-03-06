/*
 * Copyright 2014 Eediom Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.logstorage.file;

import org.araqne.storage.api.FilePath;
import org.araqne.storage.api.StorageInputStream;

public class DataBlockV3Params {
	public IndexBlockV3Header indexHeader;
	public StorageInputStream dataStream;
	public FilePath dataPath;
	public String compressionMethod;
	public String cipher;
	public String digest;
	public byte[] cipherKey;
	public byte[] digestKey;
}
