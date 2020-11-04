/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.analysis.backup.parserv2.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.ogema.serialization.jaxb.Resource;
import org.ogema.tools.impl.JsonReaderJackson;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.analysis.backup.parserv2.BackupParser;

@Component
public class BackupParserImpl implements BackupParser {
	
	private volatile JAXBContext jaxbContext;
	private final static Logger logger=  LoggerFactory.getLogger(BackupParserImpl.class);
    private JsonReaderJackson json;    
	
	@Activate
	protected void activate() throws JAXBException {
			jaxbContext = JAXBContext.newInstance("org.ogema.serialization.jaxb",
					org.ogema.serialization.jaxb.Resource.class.getClassLoader());
        json = new JsonReaderJackson();
	}
	
	@Deactivate
	protected void deactivate() {
		jaxbContext = null;
	}
	
	
	@Override
	public Resource parse(Path file) throws IOException {
		if (!Files.exists(file) || !Files.isRegularFile(file))
			throw new IllegalArgumentException("Not a regular file: " + file);
		return parse(new StreamSource(file.toFile()));
	}

	@Override
	public List<Resource> parseFolder(Path folder, boolean recursive) throws IOException {
		if (!Files.exists(folder))
			throw new IllegalArgumentException("Folder does not exist: " + folder);
		if (Files.isDirectory(folder)) 
			return parseActualFolder(folder, recursive);
		else if (Files.isRegularFile(folder)) 
			return parseZipFile(folder, recursive);
		else 
			throw new IllegalArgumentException("File " + folder + " is neither a folder nor a zip file");
	}
	
	private List<Resource> parseZipFile(Path folder, boolean recursive) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(folder, (ClassLoader)null)) {
			final Path root = fs.getPath("/");
			final int maxDepth = (recursive ? Integer.MAX_VALUE : 1);
			// sometimes fails with funny IOExceptions... see https://stackoverflow.com/questions/14654737/nosuchfileexception-while-walking-files-tree-inside-a-zip-using-java-nio
			/*return Files.walk(root, maxDepth).filter(file -> Files.isRegularFile(file) && (file.toString().endsWith(".ogx") || file.toString().endsWith(".xml")))
				.map(path -> {
					try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
						return parse(bis);
					} catch (IOException e) {
						logger.warn("Parsing file " + path + " failed. "+e);
						return null;
					}
				}).filter(resource -> resource != null).collect(Collectors.toList());
				*/
			final List<Resource> result = new ArrayList<>();
			Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), maxDepth, new java.nio.file.SimpleFileVisitor<Path>() {
				
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!Files.isRegularFile(file)) {
						return FileVisitResult.CONTINUE;
                    }
					final String filename = file.getFileName().toString().toLowerCase();
                    if (filename.endsWith(".ogx") || filename.endsWith(".xml")) {
                        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file))) {
                            final Resource r = parse(bis);
                            if (r != null) {
                                result.add(r);
                            }
                        } catch (IOException e) {
                            logger.warn("Parsing file {} failed. {}", file, e.toString());
                        }
                    } else if (filename.endsWith(".ogj") || filename.endsWith(".json")) {
                        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file))) {
                            final Resource r = parseJson(bis);
                            if (r != null) {
                                result.add(r);
                            }
                        } catch (IOException e) {
                            logger.warn("Parsing file {} failed. {}", file, e.toString());
                        }
                    }
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					logger.warn("File visit failed. {}",exc.toString());
					return FileVisitResult.CONTINUE;
				}
				
			});
			return result;
		}
	}
    
    private Resource parseAny(Path file) throws IOException {
        if (file.toString().endsWith(".ogx") || file.toString().endsWith(".xml")) {
            return parse(file);
        } else if (file.toString().endsWith(".ogj") || file.toString().endsWith(".json")) {
            return parseJson(Files.newInputStream(file));
        } else {
            return null;
        }
    }
	
	private List<Resource> parseActualFolder(Path folder, boolean recursive) throws IOException {
		List<Resource> results = Files.list(folder).filter(file -> Files.isRegularFile(file)).map(file -> {
			try {
				return parseAny(file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).filter(r -> r != null).collect(Collectors.toList());
		if (recursive) {
			Files.list(folder).filter(file -> Files.isDirectory(file)).forEach(file -> {
				try {
					results.addAll(parseFolder(file, recursive));
				} catch (IOException e) {
                    logger.debug("exception during parsing {}", file, e);
				}
			});
		}		
		return results;
	}
	
	@Override
	public Resource parse(InputStream stream) throws IOException {
		return parse(new StreamSource(stream));
	}
    
    private Resource parseJson(InputStream is) throws IOException {
        try (InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(r)) {
            return json.read(br);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
	
	@Override
	public Resource parse(String xml) throws IOException {
		return parse(new StreamSource(new StringReader(xml)));
	}

	@SuppressWarnings("rawtypes")
	private Resource parse(Source src) throws IOException {
		try {
			return jaxbContext.createUnmarshaller()
					.unmarshal(src, org.ogema.serialization.jaxb.Resource.class).getValue();
		} catch (JAXBException ex) {
			throw new IOException(ex);
		}
	}
	
	
}
