# ActiveRecJ
Simple SQL Query Builder class for Java

##Usage
Create a new instance of ActiveRecord

```
ActiveRecJ ar = ActiveRecJ.createConnection("org.sqlite.JDBC", "jdbc:sqlite:gtddata.sqlite");
```

Define fields as a string array

```
String fields = {"task_name text", "task_description text", "time text", "date text", "recurring integer", "action integer"}
```

Create a table by supplying the table name, fields list and an option whether or not to override if table exists

```
this._table = ar.createTable(tablename, fields, ActiveRecJ.OVERWRITE_FALSE);
```

Run a select query

```
ResultSet rs = this._table.select().exec();
```

It returns a `ResultSet` object that you should already be familiar with.

The object builder pattern is used and hence queries are cumulative.

```
ResultSet rs = this._table.select().where("text = 'random'").exec();
```

Limit your query results.

```
ResultSet rs = this._table.select().where("text = 'random'").limit(10).exec();
```
