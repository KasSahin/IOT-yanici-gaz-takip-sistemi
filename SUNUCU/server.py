import socket
import MySQLdb
import json

# Veritabanı bağlantısını kur ve gas, flame verilerini al
def get_data_from_db():
    try:
        # Veritabanı bağlantısı
        conn = MySQLdb.connect(
            host="localhost",  # Veritabanı sunucusu IP adresi
            user="root",  # MySQL kullanıcı adı
            passwd="123456",  # MySQL şifresi
            db="monitoring"  # Veritabanı adı
        )
        cursor = conn.cursor()

        # Veritabanından gas ve flame verilerini seçiyoruz
        cursor.execute("SELECT gas, flame FROM kontrol")
        result = cursor.fetchone()

        # Veritabanı bağlantısını kapatıyoruz
        conn.close()

        if result:
            gas, flame = result
            # Verileri JSON formatına uygun dictionary olarak döndür
            return {"mutfak": gas.strip(), "flame": flame.strip()}
        else:
            return {"error": "No data available"}  # No data için uygun mesaj
    except MySQLdb.Error as e:
        print(f"Database Error: {e}")
        return {"error": "Database connection failed"}

# Sunucu kurulumu
def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 3307))  # Sunucu IP ve port numarası
    server_socket.listen(5)
    print("Server is listening...")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"Connection from {addr}")

        try:
            # Veritabanından verileri alıp istemciye JSON formatında gönderiyoruz
            data = get_data_from_db()
            json_data = json.dumps(data)  # Veriyi JSON formatına dönüştürüyoruz
            print(f"Sending JSON data: {json_data}")  # JSON verisini kontrol et
            client_socket.sendall(json_data.encode('utf-8'))  # JSON verisini istemciye gönderiyoruz
        except Exception as e:
            print(f"Sending Error: {e}")

        finally:
            client_socket.close()  # Her durumda bağlantıyı kapat

if __name__ == "__main__":
    start_server()