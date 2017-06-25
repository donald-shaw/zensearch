
## ZenSearch - _Donald Shaw_

ZenSearch is an implementation of Zendesk's coding challenge to write a command line application that can perform
searches across inter-related collections of users, tickets and organizations.

The code is written in Scala, and makes use of the Akka library. It was developed using SBT. 
To run the project, you will need the following installed:
  - Java (I used version 8, although version 7 should work fine), 
  - Scala (I used version 2.12.2) and 
  - SBT itself (I used version 0.13.7)
  
From there the program can be run by calling 'sbt console' from the command line (or calling either of the files
'zensearch.sh' or 'zensearch.bat' as appropriate). This takes you into SBT's 'console' REPL where the project has
been set up to initiate the Akka actor system and read in and parse the json data files. From there searches can
be executed using the following rough-and-ready DSL:

&gt; **&lt;_type_> where "&lt;_field_>" is &lt;_value_>** (&lt;_value_> needs to be in quotes if it is a string)

eg.:

&gt; **users where "name" is "Rose Newton"**

or

&gt; **tickets where "submitter_id" is 71**

or

&gt; **organizations where "details" is "Non profit"**

These calls will pull up all matching records, along with some information on cross-linked records (the
specification of what fields are cross-linked between records - along with what record types there are and
which files hold that record data - is provided in a file called 'zentypes.json' in the resources directory,
along with the json record files themselves).

The data is rather simply formatted as pretty-printed json. Nicer formatting could be produced by modifying
FormattingActor.

Use 'exit' to shutdown the system and quit the console and sbt.


This is not the best as far as front-end interfaces go (there are also some extraneous messages printed out when
the system is starting up or shutting down), but it does show that the back-end indexing and searching system is
working. I had hoped to produce a better CLI by utilising something I have read about but not tried before,
namely coding custom SBT commands, which would make for a more interactive interface to the program, but
unfortunately I ran up against too many unexpected issues (eg. cross-compiler version mismatches) and ran out of
time to nut my way through them and get something working along that line (I suspect I might have to fully clear
out my dev set up and re-install everything to get past the issues). With a bit more time (which I didn't
feel I had), a better interface choice would be a web front-end (eg. written using the Play framework, talking to
Akka Http in front of the Akka actor back-end).


