# Mockingbird - birdsong study app

Mockingbird is an Android application for learning bird songs. It works
in a similar manner to flash card programs like
[Anki](https://apps.ankiweb.net/) but has some features to make it more
convienent for learning bird songs.

![Mockingbird splash screen](/docs/screenshots/splash.png?raw=true)

Mockingbird does its best to determine the relevant information about a
bird from the audio file itself, avoiding the tedious process of creating
flash cards by hand.

Unlike paid apps, Mockingbird does not include any licensed bird songs with.
It is easy to add songs from other locations, including directly from
[Xeno-Canto](http://www.xeno-canto.org/) a enormous repository of bird
songs created collaborative by recordists worldwide.

Mockingbird grew out of my frustration while taking a course on bird song.
I had a set of songs to study, but at the time I was unable to find an
app which made studying them convienent, and I ended up just playing the
songs on my phone and doing my best to not look at the filename until I had
an guessed the bird which was singing.

## Playlists

A playlist is a set of songs to be studied as a group. The group could be
for a particular location and time of year, or just birds that are easily
confused with one another, such as many North American warblers.

![Choosing a playlist](/docs/screenshots/playlists.png?raw=true)

Playlists are played in random order. The app keeps track of which songs
are identified incorrectly and plays them more often.

![Playing a playlist](/docs/screenshots/play_playlist.png?raw=true)

### Creating Playlists over USB

Mockingbird playlists are stored under /sdcard/Mockingbird. Each playlist
is stored in a separate folder. Any .mp3, .wav or .ogg file in the folder
will be considered a bird song and made part of the playlist.

Simply copying a folder containing bird songs from your computer to the
Mockingbird folder on your phone is enough to create a playlist.

### Creating Playlists from within Mockingbird

Songs can also be added from the Android *Downloads* folder or from
[Xeno-Canto](http://www.xeno-canto.org/).

![Search Xeno-Canto](/docs/screenshots/search_xeno-canto.png?raw=true)

Unfortunately, Xeno-Canto does not provide a nice way of generating search
suggestions from within an app, and it was necessary to reverse engineer
the website in order to get things working. The side effect of this is that
only English common names are supported.

### Editing Playlists

Playlists can be renamed and deleted from within Mockingbird. It is also
possible to remove a song from a playlist.

![Playing a playlist](/docs/screenshots/edit_playlist.png?raw=true)

## Development Notes

The initial version of Mockingbird only supported creating playlists over a
USB connection from a computer. This worked fine when studying a provided set
of songs, for instance from the bird song class I took, but made creating new
playlists tedious.

The next iteration supported addings songs from Downloads and from Xeno-Canto.
The [Xeno-Canto API] (http://www.xeno-canto.org/article/153) was used to
implement the search functionality. The API is particular about using precise
common names of birds (e.g. a search for goldfinch will not work, you must
specify American Goldfinch.) The website supports search suggestions, but no
public API is available, making it necessary to reverse engineer the
internal API at
http://www.xeno-canto.org/api/internal/completion/species.

The last iteration added support for keeping track of how often the user gets
songs correct or incorrect so that difficult songs could be played more often.
This involved changing the UI from Play/Show Answer/Next Song to showing
multiple choices. The multiple choices are initially random, but once mistakes
are made, will include previous mistaken choices to make things more challenging.

An initial goal was to make it possible to use the app without being able to
see the screen. Text-to-speech support was implemented to support this and
testing was done with accessibility enabled. This goal became less important
once multiple choice was implemented.

A simplified version of the
[Leitner System](https://en.wikipedia.org/wiki/Leitner_system) for spaced
repetition is used to handle choosing which song to play next. Four *bins* are used.
Incorrect songs are played in bin 0, and promoted to the next bin each time they
are identified correctly. Each song starts in bin 4.

A bin is chosen at random, with the probability proportional to the number of songs
inside the bin multiplied by a constant depending upon the bin number, with bin 0
having the largest constant. A bin is the chosen according to the calculated
probability, and then a song is chosen from within the bin. Each song within a bin
is equally likely to be chosen with the constraint that the same song should not
be played twice in a row.

This added complexity (earlier iterations just shuffled the songs once all had
been played) made it necessary to add automated tests as well as a database to
track metadata on the songs and playlists. Automated tests were written for
existing features and then as the database features were added which saved a
lot of effort in testing various situations inside the application.

At this point, the project is more or less done with the exception of bug
fixes.  I'm not sure I've ever finished a hobby project in the past. I think
the difference with Mockingbird is that I cared about using the resulting
application where as before I was more interested in learning about something.
The problem with a learning project is that once you've learned what you were
interested in learning there remains a lot of fiddly details before you're
done, which are hard to get through if you don't actually care about the
results.

## TODO

Although I think I'm done with Mockingbird, there are some nice to have
features I may get to someday:
* There is a lot of duplicated code with only small differences which handles
playing songs in each activity. This should be refactored into a common base
class.
* It would be nice to be able to do some small edits to songs that have been
downloaded, for instance to play only a subset of a song, apply a high pass
filter to remove background noise, or normalize volume.
* It would be interesting to make it possible to view the sonogram of the song
being played as a visual aid to help learn identification. This would require
using a lower level interface to the audio data and would consume more battery.
* The song randomization and learning model could almost certainly be improved.
