import MySQLdb
import RPi.GPIO as GPIO
import time

#mysql veritabanına bağlantı
db=MySQLdb.connect(
  host="localhost",
  user="root",
  passwd="123456",
  db="monitoring")
cursor=db.cursor()

#sensör pinlerinin tanımlanması
mq2_pin=17 #mq2 sensörü için kulanılan GPIO Pini
flame_pin=27 #alev sensörü için kulanılan GPIO Pini

#GPIO ayarları

GPIO.setmode(GPIO.BCM)
GPIO.setup(mq2_pin,GPIO.IN)
GPIO.setup(flame_pin,GPIO.IN)


try:
    while True:
        #mq2 sensöründen veri okuma
        mq2_status=GPIO.input(mq2_pin)
        mq2_value="gaz tespit edildi" if mq2_status==0 else "temiz hava "

        #alev sensöründen veri okuma 
        flame_status=GPIO.input(flame_pin)
        flame_value="alev tespit edildi" if flame_status==0 else "alev tespit edilmedi"
        
        #veritabanında verileri güncelleme 
        sql="UPDATE kontrol SET flame=%s,gas=%s WHERE id=1"
        values=(flame_value,mq2_value)
        cursor.execute(sql,values)

        #değişiklikleri kaydetme
        db.commit()

        print(f"Gas:{mq2_value},flame:{flame_value}")
        time.sleep(10)

except KeyboardInterrupt:
    print("program sonlandırılıyor")

finally: 
    #gpıo ve mysql bağlantısını temizleme
     GPIO.cleanup()
     db.close()
