#!/usr/bin/env python
import string
import sys
import os
# exclude some starting title
exe_start = ['Media:', 'Special:', 'Talk:', 'User:', 'User_talk:', 'Project:', 'Project_talk:', 'File:', 'File_talk:',
             'MediaWiki:', 'MediaWiki_talk', 'Template:', 'Template_talk:', 'Help:', 'Help_talk:', 'Category:',
             'Category_talk:', 'Portal:', 'Wikipedia:', 'Wikipedia_talk:']
# exclude some suffixes
exe_ending = ['.jpg', '.gif', '.png', '.JPG', '.GIF', '.PNG', '.txt', '.ico']
# exclude some pages of specific title
exe_title = ['404_error/', 'Main_Page', 'Hypertext_Transfer_Protocol', 'Search']
# lower-case letters
# low_case = 'abcdefghijklmnopqrstuvwxyz';


# A function used to check whether th title is valid

def isvalid(string):
    # checks title start with wrong words
    for s in exe_start:
        if string.startswith(s):
            return False
    # checks title start with lower-case character
    if string[0:1].islower():
        return False
    # checks title end with wrong words
    for s in exe_ending:
        if string.endswith(s):
            return False
    # check title of wrong words
    for s in exe_title:
        if s == string:
            return False
    return True

def main():
    # read every line and process it
    date = os.environ["mapreduce_map_input_file"].split('-')[-2]
    for line in sys.stdin:
        if not line:
            break
        array = line.split()
        # not valid
        if len(array) !=4 or array[0] != 'en':
            continue
        # if valid, printout all (key,value) pairs
        if isvalid(array[1]):
            print '%s\t%s' % (array[1], array[2] + '^^' + date)

if __name__ == "__main__":
    main()

