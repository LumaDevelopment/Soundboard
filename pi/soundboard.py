from gpiozero import Button
from time import sleep
import mysql.connector

button_a = Button(2)
button_b = Button(3)
button_c = Button(4)
button_d = Button(5)
button_e = Button(6)
button_f = Button(7)
button_g = Button(8)
button_h = Button(9)
button_i = Button(10)
button_j = Button(11)

def insertbutton( int ):
    mydb = mysql.connector.connect(
      host="host",
      user="user",
      passwd="password",
      database="database"
    )

    mycursor = mydb.cursor()

    sql = "INSERT INTO buttons SET button = '{0}';"
    mycursor.execute(sql.format(int))

    mydb.commit()

while True:
    if button_a.is_pressed:
        print("Button 1 Pressed")
        insertbutton(2)
    elif button_b.is_pressed:
        print("Button 2 Pressed")
        insertbutton(3)
    elif button_c.is_pressed:
        print("Button 3 Pressed")
        insertbutton(4)
    elif button_d.is_pressed:
        print("Button 4 Pressed")
        insertbutton(5)
    elif button_e.is_pressed:
        print("Button 5 Pressed")
        insertbutton(6)
    elif button_f.is_pressed:
        print("Button 6 Pressed")
        insertbutton(7)
    elif button_g.is_pressed:
        print("Button 7 Pressed")
        insertbutton(8)
    elif button_h.is_pressed:
        print("Button 8 Pressed")
        insertbutton(9)
    elif button_i.is_pressed:
        print("Button 9 Pressed")
        insertbutton(10)
    elif button_j.is_pressed:
        print("Button 10 Pressed")
        insertbutton(11)
    sleep(0.2)
