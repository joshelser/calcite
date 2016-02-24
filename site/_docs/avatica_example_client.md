---
layout: docs
title: Avatica Example Client
sidebar_title: Example Client
permalink: /docs/avatica_example_client.html
---

<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

While Avatica ships its own client in the form of a JDBC driver, Avatica also
was built to enable anyone to build their own client. Avatica's wire API primarily mimics
the JDBC API. The following examples show how the JDBC API compares to the Avatica
API calls. The Request and Response objects will be used by name only; refer to the
appropriate documentation for your deployment, either [JSON]({{ site.baseurl }}/docs/avatica_json_reference.html)
or [Protobuf]({{ site.baseurl }}/docs/avatica_protobuf_reference.html).

The following examples all assume that there is no `autoCommit` feature enabled.

# Table of Contents

<ul>
  <li><a href="#insertupdate-a-single-row">Insert/Update a single row</a></li>
  <li><a href="#inserting-many-rows">Inserting many rows</a></li>
  <li><a href="#querying-many-rows">Querying many rows</a></li>
</ul>

## Insert/Update a single row

In JDBC, we may wish to insert a single update (e.g. a new row or an update to
a column in an existing row).

{% highlight java %}
final Driver driver = ...;
// Some non-user-derived input
final String sql = "UPDATE my_table SET column_1='foo' WHERE pk=12345";
try (Connection conn = driver.connect("jdbc://..", properties);
     Statement stmt = conn.createStatement(sql)) {
  stmt.executeUpdate();
  connection.commit();
}
{% endhighlight %}

In the Avatica API, we can use the PrepareAndExecuteRequest as short-hand
to reduce the number of RPCs (combining a PrepareRequest with an ExecuteRequest):

{% highlight java %}
// Open a connection in the server
connectionId = OpenConnectionRequest()
// Sync the local state with the server's state (if necessary)
ConnectionSyncRequest()
statementId = CreateStatementRequest()

// Submit the SQL to run in a single RPC
PrepareAndExecuteRequest(connectionId, statementId, sql)
// Ensure the update was committed
CommitRequest(connectionId)

// Free the Statement
CloseStatementRequest(connectionId, statementId)
// Free the Connection
CloseConnectionRequest(connectionId)
{% endhighlight %}

## Inserting many rows

Using JDBC, we would have something like the following to insert a row of data
using a `PreparedStatement`. Let's consider a table with two columns: the first
column is an `INTEGER` and the second is a `VARCHAR`

{% highlight java %}
final Driver driver = ...;
try (Connection conn = driver.connect("jdbc://..", properties)) {
  final String sql = "INSERT INTO my_table VALUES(?, ?)"
  try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    for (int i = 0; i < 10; i++) {
      stmt.setInt(1, i);
      stmt.setString(2, Integer.toString(i));
      stmt.executeUpdate();
    }
    conn.commit();
  }
}
{% endhighlight %}

In the Avatica API, this will equate to the following requests:

{% highlight java %}
// Open a connection in the server
connectionId = OpenConnectionRequest()
// Sync the local state with the server's state (if necessary)
ConnectionSyncRequest()
statementId = CreateStatementRequest()

// Prepare the SQL to be run once
PrepareRequest(connectionId, statementId, sql)
// Execute the update with the bound-parameters ten times
for (int x = 0; x < 10; x++) {
  List bound_params = ...;
  ExecuteRequest(connectionId, statementId, bound_params)
}

// Ensure the update is committed in the server
CommitRequest(connectionId)
// Free the Statement
CloseStatementRequest(connectionId, statementId)
// Free the Connection
CloseConnectionRequest(connectionId)
{% endhighlight %}

## Querying many rows

In JDBC, say we have a table with many columns and want to read all of the
rows in that table and do some sort of processing on each row.

{% highlight java %}
Driver driver = ...;
try (Connection conn = driver.connect("jdbc://..", properties);
     Statement stmt = conn.createStatement()) {
  try (ResultSet results = stmt.executeQuery("SELECT * FROM my_table")) {
    while (results.next()) {
      ...
    }
  }
}
{% endhighlight %}

When using the Avatica API directly, we need to handle the underlying pagination
of results from the Avatica server (whereas the Avatica JDBC driver is hiding
this pagination for us).

{% highlight java %}
// Open a connection in the server
connectionId = OpenConnectionRequest()
// Sync the local state with the server's state (if necessary)
ConnectionSyncRequest()
statementId = CreateStatementRequest()

// Prepare the statement, run it and get the first batch (frame) of results.
resp = PrepareAndExecuteRequest(connectionId, statementId, sql)
offset = 0;

// Track the offset of rows returned as we process them
offset += process_frame(resp.frame);

// Fetch more batches as long as more exist to return
while (!resp.frame.done) {
  resp = FetchRequest(connectionId, statementID, offset)
  offset += process_frame(resp.frame);
}

// Free the Statement
CloseStatementRequest(connectionId, statementId)
// Free the Connection
CloseConnectionRequest(connectionId)
{% endhighlight %}
