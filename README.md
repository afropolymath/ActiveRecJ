# ActiveRecJ
Simple SQL Query Builder class for Java

##Usage
Create a new instance of ActiveRecord

```
ActiveRecJ ar = ActiveRecJ.createConnection("org.sqlite.JDBC", "jdbc:sqlite:gtddata.sqlite");
```

Define fields as an array

```
String fields = {"task_name text", "task_description text", "time text", "date text", "recurring integer", "action integer"}
```

Create a table using the fields list

```
this._table = ar.createTable(tablename, fields, ActiveRecJ.OVERWRITE_FALSE);
```