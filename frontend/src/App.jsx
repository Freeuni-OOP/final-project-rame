import { useEffect, useState } from 'react'
import './App.css'

function App() {
    const [message, setMessage] = useState('ველოდები ბექენდს...')

    useEffect(() => {
        fetch('http://localhost:8080/api/hello')
            .then(response => {
                if (!response.ok) {
                    throw new Error('სერვერმა დააბრუნა შეცდომა');
                }
                return response.text(); // რადგან ბექენდიდან უბრალო ტექსტი მოდის
            })
            .then(data => {
                setMessage(data); // ვინახავთ მიღებულ ტექსტს
            })
            .catch(error => {
                console.error("შეცდომა კავშირისას:", error);
                setMessage('ბექენდთან დაკავშირება ვერ მოხერხდა 🛑');
            });
    }, [])

    return (
        <div className="App">
            <h1>სერიალების ტრეკერი</h1>
            <div className="card">
                <p>პასუხი ბექენდიდან:</p>
                <h2>{message}</h2>
            </div>
        </div>
    )
}

export default App