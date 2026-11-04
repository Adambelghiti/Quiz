# Quiz Game: Multiplayer Trivia Challenge

A full-stack multiplayer quiz game built with Angular, Spring Boot, and WebSocket real-time communication.

## Features

- **Multiplayer**: Host and join real-time quiz rooms
- **Three Languages**: Questions in English, French, and Arabic
- **Accessibility**: Voice reading support with Web Speech API
- **Admin Panel**: Manage questions and settings
- **Real-time Updates**: Instant feedback and scoring via WebSocket
- **Responsive Design**: Works on desktop and mobile

## Setup

### Prerequisites

- Java 17+
- Node.js 20+
- npm
- Chrome or Edge (for frontend tests)

### Development Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd Quizgame
   ```

2. **Start the Backend**:
   ```bash
   cd Backend
   .\mvnw.cmd spring-boot:run
   ```
   On macOS/Linux:
   ```bash
   cd Backend
   ./mvnw spring-boot:run
   ```
   The backend runs on `http://localhost:8080`

3. **Start the Frontend**:
   ```bash
   cd ../Frontend
   npm install
   npm start
   ```
   The frontend runs on `http://localhost:4200`

4. **Access the Application**:
   Open `http://localhost:4200` in your browser

### Login Credentials

- **Player**: `demo` / `demo123`
- **Administrator**: `admin` / `admin123`

## Question Management

### Question Banks

- `new_questions.json`: Main question bank
- `new_questions_round2.json`: Additional questions
- `question-bank-expansion-3.json`: Expanded question bank (50 questions across 5 categories)

### Adding New Questions

1. Create a new JSON file with the following structure:
   ```json
   {
     "description": "Round 3 content expansion: 50 questions...",
     "categories": ["GEOGRAPHY", "SPORTS", "MUSIC", "SCIENCE", "GENERAL_KNOWLEDGE"],
     "languages": ["EN", "FR", "AR"],
     "questions": [
       {
         "category": "GEOGRAPHY",
         "difficulty": 1,
         "translations": {
           "EN": {
             "text": "Which is the largest continent by area?",
             "hint": "It contains China and India.",
             "options": ["Asia", "Africa", "North America", "Europe"],
             "correctIndex": 0,
             "explanation": "Asia is the largest continent by area and population."
           },
           "FR": {
             "text": "Quel est le plus grand continent par superficie ?",
             "hint": "Il comprend la Chine et l'Inde.",
             "options": ["Asie", "Afrique", "Amérique du Nord", "Europe"],
             "correctIndex": 0,
             "explanation": "L'Asie est le plus grand continent par superficie et par population."
           },
           "AR": {
             "text": "ما هي أكبر قارة من حيث المساحة؟",
             "hint": "تضم الصين والهند.",
             "options": ["آسيا", "أفريقيا", "أمريكا الشمالية", "أوروبا"],
             "correctIndex": 0,
             "explanation": "آسيا هي أكبر قارة من حيث المساحة وعدد السكان."
           }
         }
       }
     ]
   }
   ```

2. Add the new file to `SeedNewQuestions.java` in the `Backend` directory

3. Run the seed command:
   ```bash
   cd Backend
   .\mvnw.cmd compile
   .\mvnw.cmd exec:java -Dexec.mainClass="com.example.quiz.SeedNewQuestions"
   ```

## Audio & Voice Features

### Text-to-Speech Support

- **Languages**: English, French, Arabic
- **Voice Support**: Browser's Web Speech API
- **Accessibility**: All UI components include accessible labels

### Voice Features

- **Question Reading**: Automatically reads questions and answer choices
- **Feedback**: Reads correct/incorrect feedback with explanations
- **Timer Alerts**: Announces when time is up
- **Language Detection**: Automatically detects question language

### Customizing Voice Settings

1. In `SpeechService.ts`, modify the `languageTag` method to change BCP-47 language tags
2. Adjust speech parameters in the `speak` method:
   - `rate`: 0.95 (slightly slower than default)
   - `pitch`: 1 (normal pitch)

## Deployment

### Production Build

```bash
# Build the frontend
cd Frontend
npm run build

# Build the backend
cd ../Backend
.\mvnw.cmd package
```

### Deployment Options

1. **Docker**: Use the provided Dockerfile
2. **Cloud**: Deploy to AWS, Azure, or Google Cloud
3. **On-Premise**: Run on a local server

## Contributing

We welcome contributions! Please follow these guidelines:

1. Fork the repository
2. Create a new branch for your feature
3. Commit your changes with clear messages
4. Run tests to ensure everything works
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.