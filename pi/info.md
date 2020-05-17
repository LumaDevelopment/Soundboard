The script on the Raspberry Pi listens for GPIO input and makes SQL requests.
Language: Python

Functionality:
- The program uses a while loop to check for button presses (with runs being spaced 0.2s apart) and calls insertbutton() if a button is pressed.
- If a button is pressed, the program inputs the button's GPIO number into a SQL table