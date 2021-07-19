
## ZenSearch - _Donald Shaw_

ZenSearch is an implementation of Zendesk's coding challenge to write a command line application that can perform
searches across inter-related collections of users, tickets and organizations.

The code is written in Scala, and makes use of the Akka library. It was developed using SBT. 
To run the project, you will need the following installed:
  - Java (I used version 11.0.8, although any version 8 should work fine, too), 
  - Scala (I used version 2.12.14 - needed to be 2.12.x to avoid some library version issues) and 
  - SBT itself (I used version 1.4.3)
  
From there the program can be run by calling '_sbt run_' from the command line (actually, you will
generally want to run it with '_sbt "run -f text"_' to get text-based formatting of results), or calling either
of the files '**zensearch.sh**' or '**zensearch.bat**' as appropriate. This will compile the code (if required),
and start the app, initialising the Akka actor system, reading in and parsing the data files, and putting you
into a rough-and-ready  REPL. From there, you will be prompted on the search parameters you want - eg.:

<pre>
         *** Starting ZenSearch ***



Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): u
Choose a field to search on - or request a (l)ist of valid fields for users: _id
Enter the value for that field to search on, or just hit [Return] to search for the field bein
g empty: 71



Search results:

Record found: user - _id = '71'
_id                 71
active              true
alias               "Miss Dana"
created_at          "2016-04-18T11:05:43 -10:00"
email               "danahinton@flotonic.com"
external_id         "c972bb41-94aa-4f20-bc93-e63dbfe8d9ca"
last_login_at       "2013-05-01T01:18:48 -10:00"
locale              "zh-CN"
name                "Prince Hinton"
organization_id     121
phone               "9064-433-892"
role                "agent"
shared              false
signature           "Don't Worry Be Happy!"
suspended           false
tags                ["Davenport","Cherokee","Summertown","Clinton"]
timezone            "Samoa"
url                 "http://initech.zendesk.com/api/v2/users/71.json"
verified            false
assigned tickets:
  _id = "8ea53283-5b36-4328-9a78-f261ee90f44b":
    subject         "A Catastrophe in Sierra Leone"
submitted tickets:
  _id = "1a227508-9f39-427c-8f57-1b72f3fab87c":
    subject         "A Catastrophe in Micronesia"
  _id = "62a4326f-7114-499f-9adc-a14e99a7ffb4":
    subject         "A Drama in Wallis and Futuna Islands"
  _id = "27ab7105-e852-42f3-91a3-2d77c7a0c3fc":
    subject         "A Drama in Australia"
organization_name   "Hotc?ókes"

Records found: 1

Hit [return] to continue
Search on (o)rganizations, (u)sers, or (t)ickets (or ask for (?/h)elp or (q)uit): q

Finished - exiting ZenSearch

</pre>

These calls will pull up all matching records, along with some information on cross-linked records (the
specification of what fields are cross-linked between records - along with what record types there are and
which files hold that record data - is provided in a file called 'zentypes.json' in the resources directory,
along with the json record files themselves).

By default the data is rather simply formatted as pretty-printed json. Nicer formatting is possible by
implementing the **_Formatter_** and **_FormatterFactory_** traits - the 'text' format style specified in the
**zensearch.sh** and **zensearch.bat** files is just such an implementation (the **_TextFormatter_** class) 

Use 'q' to shutdown the system and quit the application.

