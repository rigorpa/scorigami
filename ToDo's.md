
Change name of prebuilt courses:
 Colomos --> Los Colomos
 Canadas --> El Centinela

The bottom toolbar is showing 1 1 1 for hole 11,12,13,etc.
                              1 2 3
on my Pixel 8 pro. How can we fix this so it shows up properly for all types of phones?

In the scoring page on the top card where the hole number is 'Hole x, par x'. I'd like to add the distance for the current hole below the par number.

I'd like to include the distances of the holes for a player to see when he's on the scoring page. Let's add it below the 'Par x' at the top card of the scoring page.

Let's prebuild the distances for our default courses in both meters and feet and add those below the 'Par x' at the top card of the scoring page. Here are the distances for those 2 courses.

Los Colomos
h1 - 199
h2 - 326
h3 - 185
h4 - 247
h5 - 299
h6 - 230
h7 - 269
h8 - 201
h9 - 350
h10 - 203
h11 - 195
h12 - 211
h13 - 399
h14 - 247
h15 - 201
h16 - 225
h17 - 296
h18 - 323

El Centinela
h1 - 274
h2 -220
h3 - 188
h4 -191
h5 - 202
h6 - 272
h7 - 176
h8 -202
h9 -203
h10 - 253
h11 - 244
h12 - 229
h13 - 189
h14 - 177
h15 - 260
h16 - 203
h17 - 211
h18 - 236


It seems I cannot cancel out of a scorecard. It does allow me to end the round but let's say the player had to quit the game and didn't want the round to be logged to the history. We should create a hamburger menu button that has a cancel option.

Let's add another button in the hamburger menu. Let's say the player already began a round and a friend just joined him and wants to be added to the scorecard. We should have a 'Add/Remove Player' from the menu button that gives ability to add or remove a player(s).

Also, I'd like to be able to swipe to the next hole after entering a score.

In the 'New Round' page, let's move the 'Previous Player' and listed previous players to the bottom of the screen in it's own card. Once those players are selected, it can show the way it does now which is above the 'Start Round' button.

Another change for the 'New Round' screen; let's change the text of the 'Player Name' input text box to 'Add Player'.
And when the players are selected to the round, let's move that list to right below the bold font 'Players' text and make it a vertical list instead of horizontal.

Okay, one last thing for today that I think we should tackle as it's a very important aspect of a golf game. Depending on how a hole is scored by the players; the following hole the order of who plays the hole first is sometimes changed dynamically. 
If Player 'A' scores a '3' in hole 1 and Player 'B' scores a '2'. The following hole needs to have Player 'B' showing up first. This is also true for any other players. The lowest score moves up the list to throw first depending on how the previous hole was scored. Does this make sense? Let me know if you ahve any question on that and I can clarify.

the distances are off on the courses. I think you built them as 'meters' instead of feet and converted to meters.

I am testing this in Android Studio with virtual devices. The app on the phone works near flawlessly but having some bugs on the watch.

1. The watch app when opened does instruct the user to begin the round from the phone app. This is good.

2. The watch app does show the scorecard once it is opened from the Phone. But there are some syncing issues possibly. 

3. The watch app does not reliably refresh the scores from the phone most of the time.

4. The watch app once a score is entered for a hole, if you move to the next hole, the same score is reflected on the new hole that was entered from the previous hole.

5. In the scenarion described in #4 above however if the new hole score is entered in the phone app, it immediately reflects in the watch app. Also good.

Below are the logs for both units. The steps are the following:

1.1 Open app on phone
1.2 Open app on watch
1.3 Start round on phone

2.1 Enter score of '2' for both players on Hole 1 from watch
2.2 Watch phone app refresh in real-time of the new score for Hole 1
2.3 In watch app, press right arrow to move to Hole 2. Notice that the scores for hole to are no longer blank but still reflect the previous hole.

watch logs:

phone logs:

I'd like to know update the UI a bit.

Phone app:

Here is what our current scoring page looks like.

![alt text](image.png)

1. Currently the scoring pages have buttons to reflect the number of shots. I'd like to change that back to the way the watch also has it witht the - X + option. Let's place that in the right side of the card.

2. In the same scoring page, I'd like the name of the player that is currently on the left to only be the first '3' letters of the player name and in bigger and bolder white font show up on the left-side that takes up most of the card from top to bottom.

3. The bottom bar quick launch layout is a bit too small for a phone. Let's change it to simply be a drop down to jump the desired hole. We should place it towards the right bottom of the screen with the same icon that we're using for the main page to the left of the drop down to select the hole number.

4. I'd like to add a bit more feedback when the user scrolls from the current hole to the next. Currently it's very fast and the feedback is not noticeable. What options do we have to apply that don't take too much overhead.

Once we complete these, we can move on to the watch app

Watch app:

1. Can we match the scoring style similar to the phone app with the 3 letter name font on the left and do the same - X + that the phone has.


Here are some other changes we should make.

Phone App:

1- In the 'Round History' page, I noticed that the winner of the round is highlited. Let's remove that.

2- Let's increase the font for the 'Cursive course name'

3- In card where the player score is in the middle, let's just have the over/under par score for the entire round. Removing the total number of throws. It should look like this:

     Round
      -3

4- The top card where the Hole number is read. Let's make it a bit taller and make sure there is at least 1 row space between Hole x, par x and distance. Make the 'Par x' in bold. Something like this

--------------------------------
|  H  H  OO  L     EEEEE  11   |
|  H  H O  O L     E       1   |
|  HHHH O  O L     EEEE    1   | 
|  H  H O  O L     E       1   |
|  H  H  OO  LLLLL EEEEE 11111 |
|                              |
|           Par 3 (bold)       |
|                              |
|           88m / 188ft        |
--------------------------------


Phone App:

1- In the 'Final Round' screen. Let's remove the total throws and just leave the over/under par round score. Let's write 'Final Score' atop the column.
2- In the 'Final Round' scree. Let's switch places for the buttons at the bottom that you 'Confirm and Edit Round' and 'Go Back and Edit Scores'.
3- When a player attempst to end a round, we should do a check to see if any missing scores are found for them and not have them rely on the cards we presented. We should show a pop-up with 'There are some missing scores, continue to end the round? This cannot be undone'. 

All is most likely good but 1 mistake.

1- In the 'Review Scores' screen, we placed 'Final Score' in each of players card. Let's remove those. We should have 'Final Score' in the last 'Final Standings' card at the bottom. We should also remove the number of throws column and just leave the over/under par score

Let me clear this up

You said:
"Remove the "Final Score" + vs-par header column from each PlayerReviewCard"
I actually mean to only remove the 'Final Score' text here but leave the actual score as it is.

"Rename "Final Standings" → "Final Score" in StandingsCard and drop the total throws column"

yes

"While I'm in the per-hole grid, drop the raw throw count row too — keeps it consistent with removing throws everywhere"

yes

1-In the same 'RoundReviewScreen'. The button to 'Confirm & Finish Round', let's make the color of that button in color 'red'.

2-In the 'RoundSetupScreen', for the 'Select Course' drop-down, let's have the last course played from 'Round History' shown in there first instead of alphabetical.

One last thing for today.

Also in the 'standings' card, I'm thinking of something fun. I like that they standings are in order of best round. But instead of the text 

1. Name
2. Name
3. Name

Can we do some type of Emoji that shows 'Gold', 'Silver', 'Bronze' or something similar just for the first 3 spots? The rest can have the

4. Name