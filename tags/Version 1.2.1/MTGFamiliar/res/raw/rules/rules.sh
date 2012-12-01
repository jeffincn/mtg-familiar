#!/bin/bash

rules="rules.html"

# Retrieve the HTML thinger
curl http://www.yawgatog.com/resources/magic-rules/ > $rules

# Remove external Javascript; it's way too much for a phone, even my beefy one
perl -pi -e 's/.*\<script .*//' $rules

# Remove the yawgatog; don't want yawgatog being bothered by our version
# Tons of cleanup
perl -pi -e 's/.*class=footer.*//' $rules
perl -pi -e 's/Hyperlinked //' $rules
perl -pi -e 's/ - Yawgatog.com//' $rules
perl -pi -e 's/.*class=doctitle.*//' $rules
perl -pi -e 's/.*class=group.*//' $rules
perl -pi -e 's/.*class=\"bg-ll\".*//' $rules
perl -pi -e 's/.*<a id=R.index.*/<br \/><br \/>/' $rules
perl -pi -e 's/.*td colspan=9.*//' $rules
perl -pi -e 's/.*<td><a href=\"#R\d+.*//' $rules
perl -pi -e 's/colspan=26/colspan=13/' $rules
perl -pi -e 's/<a href=\"#Rm\">M<\/a>/<a href=\"#Rm\">M<\/a><\/td><\/tr><tr>/' $rules

# Remove the stylesheets (just use the default)
perl -pi -e 's/.*\<link .*//' $rules

# Remove some stuff at the beginning (this is super duper ugly), add stylesheet
head -n 10 $rules > $rules.head
echo "</head><body>" >> $rules.head
tail +21 $rules > $rules.tail

# Smoosh together
cat $rules.head $rules.css $rules.tail > $rules

# Gzip that file up
rm $rules.gz
gzip -9 $rules

# Cleanup after ourselves
rm $rules.head $rules.tail
