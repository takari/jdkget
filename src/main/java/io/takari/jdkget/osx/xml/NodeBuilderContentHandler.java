/*-
 * Copyright (C) 2006 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.takari.jdkget.osx.xml;

import java.nio.charset.Charset;
import java.util.List;

import io.takari.jdkget.osx.io.SynchronizedRandomAccessStream;

public class NodeBuilderContentHandler extends XMLContentHandler {
  private NodeBuilder nodeBuilder;
  private SynchronizedRandomAccessStream sras;
  private Charset encoding;

  public NodeBuilderContentHandler(NodeBuilder nodeBuilder, SynchronizedRandomAccessStream sras, Charset encoding) {
    super(encoding);
    this.nodeBuilder = nodeBuilder;
    this.sras = sras;
    this.encoding = encoding;
  }

  @Override
  public void xmlDecl(String version, String encoding, Boolean standalone) {}

  @Override
  public void pi(String id, String content) {}

  @Override
  public void comment(String comment) {}

  @Override
  public void doctype(String name, ExternalID eid) {} // Needs a DTD description also

  @Override
  public void cdata(String cdata) {
    try {
      nodeBuilder.characters(cdata.toCharArray(), 0, cdata.length());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void emptyElement(String name, List<io.takari.jdkget.osx.xml.Attribute> attributes) {
    try {
      startElement(name, attributes);
      endElement(name);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void startElement(String name, List<io.takari.jdkget.osx.xml.Attribute> attributes) {
    try {
      Attribute2[] attrs = new Attribute2[attributes.size()];
      //for(int i = 0; i < attributes.length; ++i) {
      int i = 0;
      for (io.takari.jdkget.osx.xml.Attribute a : attributes) {
        attrs[i++] = new Attribute2("", a.identifier,
          "CDATA", "", a.value.toString());
      }
      // 	    org.xml.sax.ext.Attributes2Impl a2i = new org.xml.sax.ext.Attributes2Impl();
      // 	    for(org.catacombae.xml.Attribute a : attributes) {
      // 		System.err.println("id: " + a.identifier + " value: " + a.value.toString());
      // 		a2i.addAttribute("", a.identifier, a.identifier, "CDATA", a.value.toString());
      // 	    }
      nodeBuilder.startElementInternal(null, null, name, attrs);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void endElement(String name) {
    try {
      nodeBuilder.endElement(null, null, name);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void chardata(int beginLine, int beginColumn, int endLine, int endColumn) {
    nodeBuilder.characters(sras, encoding, beginLine, beginColumn, endLine, endColumn);
  }

  //     public void chardata(CharSequence data) {
  // 	try {
  // 	    //char[] ca = data.toCharArray();
  // 	    //nodeBuilder.characters(ca, 0, ca.length);
  // 	    nodeBuilder.characters(data);
  // 	} catch(Exception e) { throw new RuntimeException(e); }
  //     }
  @Override
  public void reference(String ref) {
    try {
      if (ref.startsWith("&#")) {
        // CharRef
        int[] codePoints = new int[1];
        if (ref.startsWith("&#x"))
          codePoints[0] = Integer.parseInt(ref.substring(3), 16);
        else
          codePoints[0] = Integer.parseInt(ref.substring(2), 10);
        char[] cp_ca = Character.toChars(codePoints[0]);
        nodeBuilder.characters(cp_ca, 0, cp_ca.length);
      } else
        System.out.println("WARNING: Encountered external references, which cannot be resolved with this version of the parser.");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
