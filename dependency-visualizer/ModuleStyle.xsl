<?xml version="1.0" encoding="ISO-8859-1"?>
<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://oss.oracle.com/licenses/CDDL+GPL-1.1
    or LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
<body>
<h2>Modules Imports/Exports Statistics</h2>
<table border="1">
<tr bgcolor="#9acd32">
<th>Module Name</th>
<th>Afferent</th>
<th>Efferent</th>
<th>Used</th>
<th>Unused</th>
<th>Total Imports</th>
<th>Level</th>
<th>Classes</th>
<th>Stability</th>
<th>Instability</th>
</tr>
<xsl:for-each select="ModuleStats/Module">
<tr>
<td><a href="Modules/{name}/both/{name}.svg"><xsl:value-of select="name"/></a> </td>               
<td><a href="Modules/{name}/imports/{name}.svg"><xsl:value-of select="import"/></a></td>
<td><a href="Modules/{name}/exports/{name}.svg"><xsl:value-of select="export"/></a></td>
<td><a href="Bundles.html#{name}"><xsl:value-of select="used-export"/></a></td>
<td><a href="Bundles.html#{name}"><xsl:value-of select="unused-export"/></a></td>
<td><a href="Bundles.html#{name}"><xsl:value-of select="total-import"/></a></td>
<td><a href=""><xsl:value-of select="layer"/></a></td>
<td><a href=""><xsl:value-of select="classes"/></a></td>
<td><a href=""><xsl:value-of select="stability"/></a></td>
<td><a href=""><xsl:value-of select="instability"/></a></td>
</tr>
</xsl:for-each>
</table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>

