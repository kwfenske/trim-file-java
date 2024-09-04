/*
  Trim File #3 - Trim Trailing Spaces or Tabs from Text Files
  Written by: Keith Fenske, http://kwfenske.github.io/
  Friday, 18 December 2009
  Java class name: TrimFile3
  Copyright (c) 2009 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 console application to remove trailing white space (blanks
  or tabs) from the end of each line in a plain text file.  Extra spaces
  commonly accumulate while editing source programs in a graphical compiler
  (IDE).  They aren't a problem, but they do waste file space and occasionally
  affect the appearance of programs.

  Input and output files are read and written as sequences of characters.  You
  may specify the character set encoding.  The local system's default encoding
  will be assumed if you don't choose an encoding.  The input file name is
  usually the first parameter (argument) on the command line.  An output file
  name is usually the second parameter.  If no output file is given, then
  output will be written on standard output (the console), which may be
  redirected with the ">" operator.  If no input file is given, and no option
  to read from standard input, then an error message is printed.  A typical
  command line would be:

      java  TrimFile3  -local  filename.txt  newfilename.txt

  Options may be given on the command line.  They should appear before the file
  names, but this is not strictly enforced.  Most options are related to the
  characters (bytes) used to separate lines in the text file.  Linux/UNIX and
  most newer systems want a single "newline" character (NL or 0x0A); older
  Macintosh OS 9 applications used a single "carriage return" (CR or 0x0D);
  many DOS and Windows applications still use CR immediately followed by "line
  feed" (LF, which is also 0x0A).  All of these separators are recognized on
  input; you may select the output separator.  The options are:

      -clean
          do not copy unrecognized control codes to the output file.  Extra
          control codes are normally passed through as text characters
          (unchanged).

      -code=name
          specifies both -incode and -outcode.  The default is the local
          encoding.  You may need to quote this according to your system's
          command syntax.  Please use only canonical Java character set names,
          with the exact spelling as found on the following web page:

              http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html

      -copy
          copy the input text without trimming.  Use this option to change
          character sets or line separators without removing trailing white
          space.

      -cr  (or)  -mac
          separate output lines with CR characters for Macintosh OS 9 (0x0D).

      -crlf  (or)  -dos
          separate output lines with CR/LF pairs for DOS/Windows (0x0D/0x0A).

      -help  (or)  -?
          show a summary of the command-line options and syntax.

      -incode=name
          specifies the input character set.  This option is only necessary
          when the input and output have different character sets, and does not
          apply when reading from standard input.  See the -code option first.

      -input=name
          specifies the input file name, when the name looks like an option and
          can't be given as a parameter on the command line.  You may need to
          quote this according to your system's command syntax.

      -local  (or)  -default
          use the local system's default line separator on output.

      -nl  (or)  -lf  (or)  -unix
          separate output lines with UNIX newline characters (0x0A).

      -outcode=name
          specifies the output character set.  This option is only necessary
          when the input and output have different character sets, and does not
          apply when writing to standard output.  See the -code option first.
          No error is generated if an output encoding can not represent input
          characters; a replacement character will be arbitrarily chosen.

      -output=name
          specifies the output file name, when the name looks like an option
          and can't be given as a parameter on the command line.  You may need
          to quote this according to your system's command syntax.

      -same  (or)  -asis
          use the same line separators on output as from the input (default).

      -stdin
          read input from standard input (pipe) instead of a file.  You must
          specify this option if you aren't using a file.

      -stdout
          write output on standard output (pipe) instead of a file.  This is
          the default action if an output file name is not given.

  You may use the null device for output if you only want to check that a text
  file has no trailing space (see /dev/null on Linux/UNIX or NUL: on
  DOS/Windows).  The console application will return an exit status equal to
  the number of white space characters removed (zero or more), or -1 for
  errors.  There is no graphical interface (GUI) for this program; it must be
  run from a command prompt, command shell, or terminal window.

  Apache License or GNU General Public License
  --------------------------------------------
  TrimFile3 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Restrictions and Limitations
  ----------------------------
  TrimFile does not add, remove, or otherwise detect the Unicode "byte order
  mark" (BOM, U+FEFF) found at the beginning of some files.  The BOM if present
  will be treated as printable text.  Output character sets may not recognize a
  BOM, and some such as UTF-16 will add the BOM if missing.  Consider using
  UTF-16BE or UTF-16LE to avoid an unwanted BOM.
*/

import java.io.*;                 // standard I/O

public class TrimFile3
{
  /* constants */

  static final int BUFFER_SIZE = 0x10000; // input buffer size (64K)
  static final char CHAR_CR = 0x0D; // ASCII carriage return (CR), same as '\r'
  static final char CHAR_DEL = 0x7F; // ASCII delete (DEL), no escape sequence
  static final char CHAR_LF = 0x0A; // ASCII line feed (LF), same as '\n'
  static final char CHAR_NUL = 0x00; // ASCII null byte (NUL), same as '\0'
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2009 by Keith Fenske.  Apache License or GNU GPL.";
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
//static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final int NO_CHAR = -99999; // flag when no pending input character
  static final String PROGRAM_TITLE =
    "Trim Trailing Spaces or Tabs from Text Files - by: Keith Fenske";

  /* class variables */

  static boolean mswinFlag;       // true if running on Microsoft Windows

/*
  main() method

  We run as a console application.  There is no graphical interface.
*/
  public static void main(String[] args)
  {
    int alltextUsed;              // total number of input chars in buffer
    char[] buffer;                // buffer big enough for most input lines
    int ch;                       // current input character as an integer
    boolean cleanFlag;            // true to delete unrecognized control codes
    long controlFound;            // total number of unrecognized control codes
    String firstFilename;         // first file name on command line (input?)
    boolean foundCr;              // found carriage return (CR), waiting for LF
    int i;                        // index variable
    String inputCharset;          // name of character set for input file
    String inputFilename;         // name of input file (if given)
    BufferedReader inputStream;   // input character stream for file or stdin
    char[] newlineChars;          // <newlineString> converted to char array
    int newlineSize;              // size of <newlineChars> and <newlineString>
    String newlineString;         // user's line separator if <sameFlag> false
    int nextChar;                 // character read but processing delayed
    int nonwhiteUsed;             // index of last non-white char in buffer
    String outputCharset;         // name of character set for output file
    String outputFilename;        // name of output file (if given)
    BufferedWriter outputStream;  // output character stream for file or stdout
    boolean printFlag;            // true when line is ready to print
    boolean sameFlag;             // true if output uses input line separators
    String secondFilename;        // second file name on command line (output?)
    boolean stdinFlag;            // true if reading from standard input
    boolean stdoutFlag;           // true if writing to standard output
    long trimCount;               // total number of trailing spaces or tabs
    boolean trimFlag;             // true to remove trailing white space
    String word;                  // one parameter from command line

    /* Initialize variables. */

    cleanFlag = false;            // by default, don't delete control codes
    controlFound = 0;             // no unrecognized control codes yet
    firstFilename = null;         // no first parameter found on command line
    inputCharset = null;          // no character set name for input file
    inputFilename = null;         // by default, there is no input file name
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    newlineString = "\n";         // only applies if <sameFlag> is false
    outputCharset = null;         // no character set name for output file
    outputFilename = null;        // by default, there is no output file name
    sameFlag = true;              // by default, output uses input's separators
    secondFilename = null;        // no second parameter found on command line
    stdinFlag = false;            // assume input is coming from a file
    stdoutFlag = false;           // assume output is going to a file
    trimCount = 0;                // no trailing spaces or tabs found yet
    trimFlag = true;              // by default, remove trailing white space

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a file name. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }
      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }
      else if (word.equals("-asis") || (mswinFlag && word.equals("/asis"))
        || word.equals("-same") || (mswinFlag && word.equals("/same")))
      {
        newlineString = "\n";     // only applies if <sameFlag> is false
        sameFlag = true;          // use input's separators for output
      }
      else if (word.equals("-clean") || (mswinFlag && word.equals("/clean")))
      {
        cleanFlag = true;         // delete unrecognized control codes
      }
      else if (word.startsWith("-code=")
        || (mswinFlag && word.startsWith("/code=")))
      {
        inputCharset = outputCharset = args[i].substring(6); // accept anything
      }
      else if (word.equals("-copy") || (mswinFlag && word.equals("/copy")))
      {
        trimFlag = false;         // copy input text without trimming
      }
      else if (word.equals("-cr") || (mswinFlag && word.equals("/cr"))
        || word.equals("-mac") || (mswinFlag && word.equals("/mac")))
      {
        /* This option is included for completeness.  Few systems still use
        only a carriage return (CR) as the line separator. */

        newlineString = "\r";     // CR only for Macintosh OS 9 and earlier
        sameFlag = false;         // use <newlineString> not original input
      }
      else if (word.equals("-crlf") || (mswinFlag && word.equals("/crlf"))
        || word.equals("-dos") || (mswinFlag && word.equals("/dos")))
      {
        newlineString = "\r\n";   // CR/LF combination for DOS/Windows
        sameFlag = false;         // use <newlineString> not original input
      }
      else if (word.equals("-default") || (mswinFlag && word.equals("/default"))
        || word.equals("-local") || (mswinFlag && word.equals("/local")))
      {
        newlineString = System.getProperty("line.separator"); // local default
        sameFlag = false;         // use <newlineString> not original input
      }
      else if (word.startsWith("-incode=")
        || (mswinFlag && word.startsWith("/incode=")))
      {
        inputCharset = args[i].substring(8); // accept anything here
      }
      else if (word.startsWith("-input=")
        || (mswinFlag && word.startsWith("/input=")))
      {
        inputFilename = args[i].substring(7); // accept anything here
      }
      else if (word.equals("-lf") || (mswinFlag && word.equals("/lf"))
        || word.equals("-nl") || (mswinFlag && word.equals("/nl"))
        || word.equals("-unix") || (mswinFlag && word.equals("/unix")))
      {
        newlineString = "\n";     // NL or newline character for UNIX
        sameFlag = false;         // use <newlineString> not original input
      }
      else if (word.startsWith("-outcode=")
        || (mswinFlag && word.startsWith("/outcode=")))
      {
        outputCharset = args[i].substring(9); // accept anything here
      }
      else if (word.startsWith("-output=")
        || (mswinFlag && word.startsWith("/output=")))
      {
        outputFilename = args[i].substring(8); // accept anything here
      }
      else if (word.equals("-stdin") || (mswinFlag && word.equals("/stdin")))
      {
        stdinFlag = true;         // read from standard input, not a file
      }
      else if (word.equals("-stdout") || (mswinFlag && word.equals("/stdout")))
      {
        stdoutFlag = true;        // write on standard output, not a file
      }
      else if (word.equals("-trim") || (mswinFlag && word.equals("/trim")))
      {
        trimFlag = true;          // undocumented default, opposite of -copy
      }
      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
      else
      {
        /* This parameter does not look like an option.  Assume that it is a
        file name.  We collect up to two file names and decide later which is
        which. */

        if (firstFilename == null)
          firstFilename = args[i]; // save original name, not lowercase <word>
        else if (secondFilename == null)
          secondFilename = args[i];
        else
        {
          System.err.println("Too many file names on command line: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }
    }

    /* All options and parameters have been scanned.  Now decide if we have the
    right number of file names, and whether they are for input or for output.
    Note that the default value is false for both <stdinFlag> and <stdoutFlag>.
    The user can only select options to turn these flags on, not off. */

    if (inputFilename != null)    // was file name given as -input= option?
    {
      if (stdinFlag)              // yes, and was -stdin option also found?
      {
        System.err.println("Can't use -input and -stdin together.");
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
    }
    else if (stdinFlag)           // no -input= option, but -stdin given?
    {
      /* do nothing */
    }
    else if (firstFilename != null) // use first parameter on command line?
    {
      inputFilename = firstFilename; // yes, save first parameter for input
      firstFilename = null;       // first parameter no longer available
    }
    else                          // we need something, anything for input
    {
      System.err.println("Missing input file name.");
      showHelp();                 // show help summary
      System.exit(EXIT_FAILURE);  // exit application after printing help
    }

    if (outputFilename != null)   // was file name given as -output= option?
    {
      if (stdoutFlag)             // yes, and was -stdout option also found?
      {
        System.err.println("Can't use -output and -stdout together.");
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
    }
    else if (stdoutFlag)          // no -output= option, but -stdout given?
    {
      /* do nothing */
    }
    else if (firstFilename != null) // use first parameter on command line?
    {
      outputFilename = firstFilename; // yes, save first parameter for output
      firstFilename = null;       // first parameter no longer available
    }
    else if (secondFilename != null) // no first parameter, how about second?
    {
      outputFilename = secondFilename; // yes, save second parameter for output
      secondFilename = null;      // second parameter no longer available
    }
    else                          // we need something, anything for output
    {
      stdoutFlag = true;          // default to writing on standard output
    }

    if (firstFilename != null)    // is there an unused first parameter?
    {
      System.err.println("Too many file names on command line: "
        + firstFilename);
      showHelp();                 // show help summary
      System.exit(EXIT_FAILURE);  // exit application after printing help
    }
    else if (secondFilename != null) // is there an unused second parameter?
    {
      System.err.println("Too many file names on command line: "
        + secondFilename);
      showHelp();                 // show help summary
      System.exit(EXIT_FAILURE);  // exit application after printing help
    }

    /* Read lines from the input.  Buffer each line until we see a newline
    character.  Then throw away any spaces at the end of the line.  We do this
    by putting input characters into a buffer and keeping two counts: one for
    the total number of characters, and another for the index (count) of the
    last character that is not white space.  The non-white counter will lag
    behind the total while skipping blanks or tabs. */

    try                           // catch file I/O errors, bad file names, etc
    {
      /* Open the input and output files.  Using BufferedReader is many times
      faster than using an InputStream directly, even though we still call
      read() for one character at a time.  The same is done for output. */

      if (stdinFlag)              // do we need to open an input file?
        inputStream = new BufferedReader(new InputStreamReader(System.in));
      else if (inputCharset == null) // reading file with default encoding?
        inputStream = new BufferedReader(new FileReader(inputFilename));
      else                        // user specified a character set encoding
        inputStream = new BufferedReader(new InputStreamReader(new
          FileInputStream(inputFilename), inputCharset));

      if (stdoutFlag)             // do we need to open an output file?
        outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
      else if (outputCharset == null) // writing file with default encoding?
        outputStream = new BufferedWriter(new FileWriter(outputFilename));
      else                        // user specified a character set encoding
        outputStream = new BufferedWriter(new OutputStreamWriter(new
          FileOutputStream(outputFilename), outputCharset));

      /* Read one character at a time.  We recognize the end of a line whenever
      we find a DOS carriage return (CR or 0x0D) by itself, a DOS line feed (LF
      or 0x0A), a UNIX newline character (NL, also 0x0A), or a DOS CR/LF pair.
      This works correctly if the input has consistent line separators.  For a
      discussion of various newline characters, see this Wikipedia web page:

          http://en.wikipedia.org/wiki/Newline

      We are careful about not overflowing the buffer with text, but assume
      there is extra space at the end for appending trailing characters needed
      for a line separator.  Two characters are enough for ASCII CR/LF.  Since
      someone may get fancy later, so we bump this margin up to eight. */

      alltextUsed = 0;            // total number of text characters in buffer
      buffer = new char[BUFFER_SIZE + 8]; // buffer for one input line + CR/LF
//    controlFound = 0;           // no unrecognized control codes yet
      foundCr = false;            // cancel any stray carriage returns
      newlineChars = newlineString.toCharArray();
                                  // convert line separator to character array
      newlineSize = newlineChars.length; // length of user's line separator
      nextChar = NO_CHAR;         // no pending input character
      nonwhiteUsed = 0;           // index of last non-white char in buffer
      printFlag = false;          // set this flag when line is ready to print
//    trimCount = 0;              // no trailing spaces or tabs found yet

      while (true)                // loop ends with end-of-file <break>
      {
        /* After reading a CR, we need to read one more character before we
        know if the CR is part of a CR/LF pair.  If it isn't, the character
        after the CR is saved in <nextChar> to be processed later. */

        if (nextChar != NO_CHAR)  // an unused character already read?
        {
          ch = nextChar;          // yes, use pending character, don't read
          nextChar = NO_CHAR;     // and clear the pending character
        }
        else                      // no pending character, read from file
          ch = inputStream.read(); // returns next character or end-of-file

        /* Other than the few control codes that we recognize (space, tab, end
        of line, etc), almost all ASCII control codes from 0x00 to 0x1F (and
        0x7F) are no longer used.  Finding them in a plain text file is
        unexpected and should be reported to the user.

        The following accounts for very old systems that put extra DEL or NUL
        bytes between the CR and LF in a CR/LF pair.  This was done for timing
        purposes (as a delay) on mechanical terminals.  It is almost never seen
        today, and would otherwise result in double spacing of our output text
        with one newline for the CR and another newline for the LF.  The -clean
        option must appear on the command line and only DEL and NUL get advance
        treatment (pun: figure it out), not other unwanted control codes. */

        if (cleanFlag && ((ch == CHAR_DEL) || (ch == CHAR_NUL)))
        {
          controlFound ++;        // count and remove extra deletes or nulls
        }
        else if (foundCr && (ch != CHAR_LF)) // does line end with CR only?
        {
          trimCount += (alltextUsed - nonwhiteUsed); // count spaces or tabs
          if (trimFlag)           // do we remove trailing white space?
            alltextUsed = nonwhiteUsed; // yes, trim spaces or tabs
          if (sameFlag)           // do we keep original line separators?
          {
            buffer[alltextUsed ++] = CHAR_CR; // insert previous CR
          }
          else                    // user has chosen different line separators
          {
            for (i = 0; i < newlineSize; i ++)
              buffer[alltextUsed ++] = newlineChars[i];
          }
          foundCr = false;        // cancel any stray carriage returns
          nextChar = ch;          // save current char or EOF for next loop
          printFlag = true;       // and print this line
        }
        else if (ch < 0)          // end of file without final CR or LF?
          break;                  // yes, exit from <while> read loop
        else if (ch == CHAR_CR)   // carriage return, maybe with LF later
          foundCr = true;         // can't do anything until next character
        else if (ch == CHAR_LF)   // line ends with LF (NL), maybe CR before?
        {
          trimCount += (alltextUsed - nonwhiteUsed); // count spaces or tabs
          if (trimFlag)           // do we remove trailing white space?
            alltextUsed = nonwhiteUsed; // yes, trim spaces or tabs
          if (sameFlag)           // do we keep original line separators?
          {
            if (foundCr)          // was previous character a carriage return?
              buffer[alltextUsed ++] = CHAR_CR; // insert previous CR before LF
            buffer[alltextUsed ++] = CHAR_LF; // insert DOS LF or UNIX NL
          }
          else                    // user has chosen different line separators
          {
            for (i = 0; i < newlineSize; i ++)
              buffer[alltextUsed ++] = newlineChars[i];
          }
          foundCr = false;        // cancel any stray carriage returns
          printFlag = true;       // and print this line
        }
        else if ((ch == '\u0009') || (ch == '\u0020') || (ch == '\u3000'))
                                  // short list of Unicode spaces, tabs
                                  // see also: isSpaceChar() isWhitespace()
        {
          buffer[alltextUsed ++] = (char) ch; // put white space into buffer
                                  // but don't increment non-white position
        }
        else if (Character.isISOControl((char) ch)) // other Unicode controls?
        {
          controlFound ++;        // count number of unexpected control codes
          if (cleanFlag == false) // do we keep these control codes?
          {
            buffer[alltextUsed ++] = (char) ch; // put character into buffer
            nonwhiteUsed = alltextUsed; // and the non-white total is the same
          }
        }
        else                      // keep all regular text characters
        {
          buffer[alltextUsed ++] = (char) ch; // put character into buffer
          nonwhiteUsed = alltextUsed; // and the non-white total is the same
        }

        /* Are we ready to print this line?  Has the input become too big for
        our buffer?  If so, break the input line and print what we have so far.
        We don't know what follows, so print everything, even white space.
        It's not possible for the buffer to fill up after a pending carriage
        return, so we can ignore the <foundCr> flag. */

        if (printFlag || (alltextUsed >= BUFFER_SIZE))
        {
          outputStream.write(buffer, 0, alltextUsed); // our line separator
          alltextUsed = nonwhiteUsed = 0; // reset character counts
          printFlag = false;      // don't print the same line again
        }
      }

      /* We are at the end of the file.  If there is anything important in
      <buffer>, then print it without line separators.  We got here because
      there wasn't a line separator after the last line, so don't add anything
      new to the output! */

      trimCount += (alltextUsed - nonwhiteUsed); // count spaces or tabs
      if (trimFlag)               // do we remove trailing white space?
        alltextUsed = nonwhiteUsed; // yes, trim spaces or tabs
      if (alltextUsed > 0)        // is there anything to print?
      {
        outputStream.write(buffer, 0, alltextUsed); // write partial line
      }

      /* Close the input and output streams, whether files or stdin/stdout. */

      inputStream.close();        // try to close input file or stdin
      outputStream.close();       // try to close output file or stdout
    }
    catch (UnsupportedEncodingException uee)
    {
      System.err.println("Unsupported character set: " + uee.getMessage());
      System.exit(EXIT_FAILURE);  // exit from application with error status
    }
    catch (IOException ioe)
    {
      System.err.println("File I/O error: " + ioe.getMessage());
      System.exit(EXIT_FAILURE);  // exit from application with error status
    }

    /* Print a summary of how many trailing spaces or tabs were found. */

    System.err.println();         // blank line, or start new line on console
    if (trimCount > 1)            // two or more
      System.err.println((trimFlag ? "Deleted " : "Copied ") + trimCount
        + " trailing spaces or tabs.");
    else if (trimCount > 0)       // exactly one
      System.err.println((trimFlag ? "Deleted " : "Copied ")
        + "one trailing space or tab.");
    else                          // none at all
      System.err.println("No trailing spaces or tabs found.");

    /* If we found any unrecognized control codes, report this to the user.  We
    don't identify which codes were found or how many of each, and we aren't as
    careful about singular or plural text as in the above message. */

    if (controlFound > 0)         // were there any unexpected control codes?
      System.err.println((cleanFlag ? "Deleted " : "Copied ") + controlFound
        + " control codes that should not appear in plain text files.");

    /* Exit from this application with a count of the trailing white space. */

    System.exit((int) Math.min(Integer.MAX_VALUE, trimCount));

  } // end of main() method

/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  TrimFile3  [options]  inputfile  [outputfile]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -clean = do not copy unrecognized control codes to the output file");
    System.err.println("  -code=name - specifies both -incode and -outcode; default is local system");
    System.err.println("  -copy = copy text without trimming; default removes trailing white space");
    System.err.println("  -cr = separate output lines with CR characters for Macintosh OS 9 (0x0D)");
    System.err.println("  -crlf = separate output lines with CR/LF pairs for DOS/Windows (0x0D/0x0A)");
    System.err.println("  -incode=name - specifies the input character set; default is local system");
    System.err.println("  -input=name - specifies the input file name; default is first parameter");
    System.err.println("  -local = use the local system's default line separator on output");
    System.err.println("  -nl = separate output lines with UNIX newline characters (0x0A)");
    System.err.println("  -outcode=name - specifies the output character set; default is local system");
    System.err.println("  -output=name - specifies the output file name; default is second parameter");
    System.err.println("  -same = use the same line separators on output as from the input (default)");
    System.err.println("  -stdin = read input from standard input (pipe) instead of a file");
    System.err.println("  -stdout = write output on standard output (pipe) instead of a file");
//  System.err.println("  -trim = default action to remove trailing white space; opposite of -copy");
    System.err.println();
    System.err.println("Standard output may be redirected with the \">\" operator.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method

} // end of TrimFile3 class

/* Copyright (c) 2009 by Keith Fenske.  Apache License or GNU GPL. */
