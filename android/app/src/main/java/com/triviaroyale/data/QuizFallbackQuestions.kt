package com.triviaroyale.data

/**
 * Minimal embedded fallback questions (~200) used only when:
 * - No internet connection AND no local cache (fresh install offline)
 * 
 * This is ~50 KB vs the old 2.2 MB static bank.
 */
object QuizFallbackQuestions {

    private val fallbackByGenre: Map<String, List<Question>> by lazy {
        mapOf(
            "General Knowledge" to generalKnowledge,
            "Sports" to sports,
            "Science" to science,
            "History" to history,
            "Geography" to geography,
            "Movies" to movies,
            "Music" to music,
            "Entertainment" to entertainment,
            "Tech" to tech,
            "Art" to art,
            "Literature" to literature,
            "Pop Culture" to popCulture,
            "IPL" to ipl
        )
    }

    private val categoryNamesByGenre: Map<String, List<String>> = mapOf(
        "General Knowledge" to listOf("General Knowledge Spotlight"),
        "Sports" to listOf("Sports Trivia"),
        "Science" to listOf("Science & Nature"),
        "History" to listOf("World History"),
        "Geography" to listOf("World Geography"),
        "Movies" to listOf("Film Trivia"),
        "Music" to listOf("Music Trivia"),
        "Entertainment" to listOf("Television"),
        "Tech" to listOf("Computer Science"),
        "Art" to listOf("Art & Design"),
        "Literature" to listOf("Books & Literature"),
        "Pop Culture" to listOf("Video Games", "Comics"),
        "IPL" to listOf("IPL Cricket")
    )

    fun getQuestions(genre: String?, category: String? = null, count: Int = 10): List<Question> {
        val pool = if (genre != null) {
            fallbackByGenre[genre] ?: generalKnowledge
        } else {
            generalKnowledge
        }
        return pool.shuffled().take(count.coerceAtMost(pool.size))
    }

    fun getCategoryNames(genre: String): List<String> {
        return categoryNamesByGenre[genre].orEmpty()
    }

    fun getQuestionCountForGenre(genre: String): Int {
        return fallbackByGenre[genre]?.size ?: 0
    }

    fun getAllGenreQuestions(): List<Question> {
        return fallbackByGenre.values.flatten()
    }

    // ── General Knowledge (20 questions) ──────────────────────────────────────
    private val generalKnowledge = listOf(
        Question("What is the capital of France?", listOf("London", "Berlin", "Paris", "Madrid"), 2, 0.1),
        Question("Which planet is known as the Red Planet?", listOf("Venus", "Mars", "Jupiter", "Saturn"), 1, 0.1),
        Question("What is the largest ocean on Earth?", listOf("Atlantic", "Indian", "Arctic", "Pacific"), 3, 0.2),
        Question("How many continents are there?", listOf("5", "6", "7", "8"), 2, 0.1),
        Question("What gas do plants absorb from the atmosphere?", listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen"), 1, 0.2),
        Question("Which is the longest river in the world?", listOf("Amazon", "Nile", "Mississippi", "Yangtze"), 1, 0.3),
        Question("What is the chemical symbol for gold?", listOf("Go", "Gd", "Au", "Ag"), 2, 0.3),
        Question("Which country has the highest population?", listOf("USA", "India", "China", "Brazil"), 1, 0.2),
        Question("What year did World War II end?", listOf("1943", "1944", "1945", "1946"), 2, 0.2),
        Question("Which animal is the fastest on land?", listOf("Lion", "Cheetah", "Horse", "Gazelle"), 1, 0.1),
        Question("What is the hardest natural substance on Earth?", listOf("Gold", "Iron", "Diamond", "Platinum"), 2, 0.2),
        Question("How many bones are in the human body?", listOf("186", "206", "226", "256"), 1, 0.3),
        Question("What is the smallest country in the world?", listOf("Monaco", "Vatican City", "San Marino", "Liechtenstein"), 1, 0.3),
        Question("Which element has the chemical symbol O?", listOf("Osmium", "Oxygen", "Gold", "Oganesson"), 1, 0.1),
        Question("What is the currency of Japan?", listOf("Yuan", "Won", "Yen", "Rupee"), 2, 0.2),
        Question("Which organ pumps blood through the body?", listOf("Brain", "Liver", "Heart", "Lungs"), 2, 0.1),
        Question("How many degrees are in a circle?", listOf("180", "270", "360", "420"), 2, 0.2),
        Question("Which blood type is a universal donor?", listOf("A+", "B+", "AB+", "O-"), 3, 0.4),
        Question("What is the main ingredient in guacamole?", listOf("Tomato", "Avocado", "Lime", "Onion"), 1, 0.1),
        Question("Which planet has the most moons?", listOf("Jupiter", "Saturn", "Uranus", "Neptune"), 1, 0.4)
    )

    // ── Sports (15 questions) ─────────────────────────────────────────────────
    private val sports = listOf(
        Question("How many players are on a soccer team on the field?", listOf("9", "10", "11", "12"), 2, 0.1),
        Question("In which sport is the term 'love' used for a score of zero?", listOf("Badminton", "Tennis", "Squash", "Volleyball"), 1, 0.2),
        Question("Which country won the FIFA World Cup 2022?", listOf("France", "Brazil", "Argentina", "Germany"), 2, 0.2),
        Question("How many points is a touchdown worth in American football?", listOf("3", "5", "6", "7"), 2, 0.2),
        Question("What is the national sport of Canada?", listOf("Hockey", "Lacrosse", "Baseball", "Curling"), 1, 0.4),
        Question("How many rings are on the Olympic flag?", listOf("4", "5", "6", "7"), 1, 0.1),
        Question("In basketball, how many points is a free throw worth?", listOf("1", "2", "3", "4"), 0, 0.1),
        Question("Which cricketer is known as the 'God of Cricket'?", listOf("Virat Kohli", "Sachin Tendulkar", "MS Dhoni", "Ricky Ponting"), 1, 0.1),
        Question("What is the maximum break in snooker?", listOf("130", "140", "147", "155"), 2, 0.4),
        Question("Which country has won the most Cricket World Cups?", listOf("India", "Australia", "West Indies", "England"), 1, 0.3),
        Question("In which year were the first modern Olympic Games held?", listOf("1892", "1896", "1900", "1904"), 1, 0.3),
        Question("What sport uses a shuttlecock?", listOf("Tennis", "Badminton", "Squash", "Table Tennis"), 1, 0.1),
        Question("How long is a marathon in kilometers?", listOf("21.1", "36.5", "42.195", "50.0"), 2, 0.3),
        Question("Which team has won the most NBA championships?", listOf("LA Lakers", "Boston Celtics", "Chicago Bulls", "Golden State Warriors"), 1, 0.4),
        Question("In golf, what is one under par called?", listOf("Eagle", "Birdie", "Bogey", "Albatross"), 1, 0.3)
    )

    // ── Science (18 questions) ────────────────────────────────────────────────
    private val science = listOf(
        Question("What is the chemical formula for water?", listOf("H2O2", "H2O", "CO2", "NaCl"), 1, 0.1),
        Question("What planet is closest to the Sun?", listOf("Venus", "Earth", "Mercury", "Mars"), 2, 0.1),
        Question("What is the speed of light approximately?", listOf("300,000 km/s", "150,000 km/s", "500,000 km/s", "1,000,000 km/s"), 0, 0.3),
        Question("What is the powerhouse of the cell?", listOf("Nucleus", "Ribosome", "Mitochondria", "Golgi Body"), 2, 0.1),
        Question("Which gas makes up about 78% of Earth's atmosphere?", listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Argon"), 2, 0.3),
        Question("What is the atomic number of carbon?", listOf("4", "6", "8", "12"), 1, 0.3),
        Question("What type of rock is formed by volcanic activity?", listOf("Sedimentary", "Metamorphic", "Igneous", "Limestone"), 2, 0.3),
        Question("What is the largest organ in the human body?", listOf("Heart", "Liver", "Brain", "Skin"), 3, 0.2),
        Question("How many chromosomes do humans have?", listOf("23", "46", "44", "48"), 1, 0.3),
        Question("What does DNA stand for?", listOf("Deoxyribonucleic Acid", "Dinitrogen Acid", "Dual Nucleic Acid", "Dynamic Neutron Array"), 0, 0.2),
        Question("Which force keeps planets in orbit around the Sun?", listOf("Magnetism", "Friction", "Gravity", "Nuclear Force"), 2, 0.2),
        Question("What is the boiling point of water in Celsius?", listOf("90", "100", "110", "120"), 1, 0.1),
        Question("Which animal is known for its ability to regenerate limbs?", listOf("Starfish", "Octopus", "Lizard", "Crab"), 0, 0.3),
        Question("What is the pH of pure water?", listOf("5", "7", "9", "14"), 1, 0.2),
        Question("Which planet has a Great Red Spot?", listOf("Mars", "Saturn", "Jupiter", "Neptune"), 2, 0.2),
        Question("What is the smallest unit of matter?", listOf("Molecule", "Cell", "Atom", "Electron"), 2, 0.3),
        Question("What vitamin does the body produce when exposed to sunlight?", listOf("Vitamin A", "Vitamin B", "Vitamin C", "Vitamin D"), 3, 0.2),
        Question("What is the most abundant element in the universe?", listOf("Oxygen", "Carbon", "Hydrogen", "Helium"), 2, 0.3)
    )

    // ── History (15 questions) ────────────────────────────────────────────────
    private val history = listOf(
        Question("Who was the first President of the United States?", listOf("John Adams", "Thomas Jefferson", "George Washington", "Benjamin Franklin"), 2, 0.1),
        Question("In what year did the Titanic sink?", listOf("1909", "1912", "1915", "1920"), 1, 0.2),
        Question("Which ancient civilization built the pyramids?", listOf("Roman", "Greek", "Egyptian", "Mesopotamian"), 2, 0.1),
        Question("Who discovered America in 1492?", listOf("Vasco da Gama", "Christopher Columbus", "Ferdinand Magellan", "Marco Polo"), 1, 0.1),
        Question("What was the longest war in history by some counts?", listOf("World War I", "Hundred Years' War", "Vietnam War", "Thirty Years' War"), 1, 0.4),
        Question("Who painted the Mona Lisa?", listOf("Michelangelo", "Leonardo da Vinci", "Raphael", "Donatello"), 1, 0.1),
        Question("Which empire was ruled by Julius Caesar?", listOf("Greek", "Roman", "Ottoman", "Persian"), 1, 0.1),
        Question("The French Revolution began in which year?", listOf("1776", "1789", "1799", "1804"), 1, 0.3),
        Question("Who was the first person to step on the Moon?", listOf("Buzz Aldrin", "Neil Armstrong", "John Glenn", "Yuri Gagarin"), 1, 0.1),
        Question("Which wall divided Berlin from 1961 to 1989?", listOf("Iron Curtain", "Berlin Wall", "Great Wall", "Hadrian's Wall"), 1, 0.1),
        Question("In which year did India gain independence?", listOf("1945", "1946", "1947", "1950"), 2, 0.1),
        Question("Who was known as Mahatma Gandhi?", listOf("Jawaharlal Nehru", "Mohandas Gandhi", "Subhas Bose", "Vallabhbhai Patel"), 1, 0.1),
        Question("The Renaissance began in which country?", listOf("France", "England", "Italy", "Germany"), 2, 0.3),
        Question("Who wrote the Declaration of Independence?", listOf("George Washington", "Benjamin Franklin", "Thomas Jefferson", "John Adams"), 2, 0.2),
        Question("What ancient wonder was located in Alexandria?", listOf("Colossus", "Hanging Gardens", "Lighthouse", "Temple of Artemis"), 2, 0.4)
    )

    // ── Geography (15 questions) ──────────────────────────────────────────────
    private val geography = listOf(
        Question("What is the largest continent?", listOf("Africa", "North America", "Asia", "Europe"), 2, 0.1),
        Question("Which desert is the largest in the world?", listOf("Gobi", "Kalahari", "Sahara", "Antarctic"), 3, 0.4),
        Question("What is the capital of Australia?", listOf("Sydney", "Melbourne", "Canberra", "Perth"), 2, 0.3),
        Question("Which river flows through London?", listOf("Seine", "Thames", "Danube", "Rhine"), 1, 0.2),
        Question("Mount Everest is located on the border of which two countries?", listOf("India and China", "Nepal and China", "Nepal and India", "China and Pakistan"), 1, 0.3),
        Question("Which country has the most time zones?", listOf("Russia", "USA", "France", "China"), 2, 0.5),
        Question("What is the smallest continent?", listOf("Europe", "Antarctica", "Australia", "South America"), 2, 0.2),
        Question("Which African country has the largest population?", listOf("South Africa", "Egypt", "Nigeria", "Ethiopia"), 2, 0.3),
        Question("What strait separates Europe from Africa?", listOf("Hormuz", "Malacca", "Gibraltar", "Bosphorus"), 2, 0.4),
        Question("Which ocean is the deepest?", listOf("Atlantic", "Indian", "Pacific", "Southern"), 2, 0.3),
        Question("What is the capital of Brazil?", listOf("Rio de Janeiro", "São Paulo", "Brasília", "Salvador"), 2, 0.3),
        Question("The Great Barrier Reef is located off the coast of which country?", listOf("Indonesia", "Australia", "Philippines", "Brazil"), 1, 0.2),
        Question("Which country is known as the Land of the Rising Sun?", listOf("China", "South Korea", "Japan", "Thailand"), 2, 0.1),
        Question("Which lake is the deepest in the world?", listOf("Lake Victoria", "Lake Baikal", "Caspian Sea", "Lake Superior"), 1, 0.4),
        Question("What is the capital of Canada?", listOf("Toronto", "Vancouver", "Ottawa", "Montreal"), 2, 0.2)
    )

    // ── Movies (15 questions) ─────────────────────────────────────────────────
    private val movies = listOf(
        Question("Who directed Titanic?", listOf("Steven Spielberg", "James Cameron", "Ridley Scott", "Martin Scorsese"), 1, 0.2),
        Question("Which movie features the character 'Jack Sparrow'?", listOf("The Mummy", "Pirates of the Caribbean", "Treasure Island", "Master and Commander"), 1, 0.1),
        Question("What is the highest-grossing film of all time?", listOf("Avengers: Endgame", "Avatar", "Titanic", "Star Wars: The Force Awakens"), 1, 0.3),
        Question("Who played the Joker in The Dark Knight?", listOf("Jack Nicholson", "Jared Leto", "Heath Ledger", "Joaquin Phoenix"), 2, 0.2),
        Question("In which year was the first Harry Potter film released?", listOf("1999", "2000", "2001", "2002"), 2, 0.3),
        Question("What is the name of the fictional African country in Black Panther?", listOf("Zamunda", "Wakanda", "Genosha", "Latveria"), 1, 0.2),
        Question("Which film won the Best Picture Oscar in 2020?", listOf("1917", "Joker", "Parasite", "Once Upon a Time"), 2, 0.4),
        Question("Who voiced Woody in Toy Story?", listOf("Tim Allen", "Tom Hanks", "Billy Crystal", "Robin Williams"), 1, 0.2),
        Question("What is Indiana Jones' real first name?", listOf("Henry", "James", "William", "Robert"), 0, 0.5),
        Question("Which studio made the film Frozen?", listOf("Pixar", "DreamWorks", "Walt Disney Animation", "Illumination"), 2, 0.2),
        Question("In The Matrix, what color pill does Neo take?", listOf("Blue", "Red", "Green", "White"), 1, 0.2),
        Question("Who directed Jurassic Park?", listOf("George Lucas", "James Cameron", "Steven Spielberg", "Ridley Scott"), 2, 0.2),
        Question("What year was the first Star Wars film released?", listOf("1975", "1977", "1979", "1980"), 1, 0.3),
        Question("Which superhero is also known as 'The Dark Knight'?", listOf("Superman", "Batman", "Spider-Man", "Iron Man"), 1, 0.1),
        Question("What was the first Pixar film?", listOf("Finding Nemo", "A Bug's Life", "Toy Story", "Monsters Inc"), 2, 0.2)
    )

    // ── Music (15 questions) ──────────────────────────────────────────────────
    private val music = listOf(
        Question("Which band performed 'Bohemian Rhapsody'?", listOf("The Beatles", "Led Zeppelin", "Queen", "Pink Floyd"), 2, 0.1),
        Question("What instrument has 88 keys?", listOf("Guitar", "Violin", "Piano", "Harp"), 2, 0.1),
        Question("Who is the 'King of Pop'?", listOf("Elvis Presley", "Michael Jackson", "Prince", "Freddie Mercury"), 1, 0.1),
        Question("Which genre originated in Jamaica?", listOf("Blues", "Jazz", "Reggae", "Salsa"), 2, 0.2),
        Question("How many strings does a standard guitar have?", listOf("4", "5", "6", "7"), 2, 0.1),
        Question("Who sang 'Shape of You'?", listOf("Justin Bieber", "Ed Sheeran", "Bruno Mars", "The Weeknd"), 1, 0.1),
        Question("Which classical composer became deaf later in life?", listOf("Mozart", "Bach", "Beethoven", "Chopin"), 2, 0.2),
        Question("What does 'forte' mean in music?", listOf("Soft", "Loud", "Fast", "Slow"), 1, 0.2),
        Question("Which country is K-pop from?", listOf("Japan", "China", "South Korea", "Thailand"), 2, 0.1),
        Question("Who wrote the national anthem of India?", listOf("Bankim Chandra", "Rabindranath Tagore", "Mahatma Gandhi", "Jawaharlal Nehru"), 1, 0.2),
        Question("What is the highest female singing voice?", listOf("Alto", "Mezzo-Soprano", "Soprano", "Contralto"), 2, 0.3),
        Question("Which instrument is Ravi Shankar famous for?", listOf("Tabla", "Sitar", "Flute", "Veena"), 1, 0.2),
        Question("What music festival is held annually in Glastonbury?", listOf("Coachella", "Glastonbury Festival", "Lollapalooza", "Tomorrowland"), 1, 0.2),
        Question("Which rapper's real name is Marshall Mathers?", listOf("Jay-Z", "Eminem", "Drake", "Kanye West"), 1, 0.2),
        Question("What is the term for a song performed by one person?", listOf("Duet", "Trio", "Solo", "Choir"), 2, 0.1)
    )

    // ── Entertainment (15 questions) ──────────────────────────────────────────
    private val entertainment = listOf(
        Question("Which TV show features dragons and the Iron Throne?", listOf("The Witcher", "Game of Thrones", "Lord of the Rings", "Vikings"), 1, 0.1),
        Question("What is the name of Homer Simpson's wife?", listOf("Marge", "Lisa", "Maggie", "Patty"), 0, 0.1),
        Question("Which streaming service created 'Stranger Things'?", listOf("Amazon Prime", "Hulu", "Netflix", "Disney+"), 2, 0.1),
        Question("In Friends, what is the name of Ross's first wife?", listOf("Rachel", "Carol", "Emily", "Julie"), 1, 0.3),
        Question("Who hosts 'The Tonight Show' since 2014?", listOf("Jimmy Kimmel", "Jimmy Fallon", "Stephen Colbert", "Conan O'Brien"), 1, 0.3),
        Question("What is the longest-running animated TV show in the US?", listOf("Family Guy", "South Park", "The Simpsons", "SpongeBob"), 2, 0.2),
        Question("Which reality show features contestants on an island?", listOf("Big Brother", "Survivor", "The Amazing Race", "Fear Factor"), 1, 0.2),
        Question("In Breaking Bad, what does Walter White teach?", listOf("Physics", "Chemistry", "Biology", "Math"), 1, 0.2),
        Question("Which show is set in the fictional town of Hawkins?", listOf("Dark", "Stranger Things", "The OA", "Riverdale"), 1, 0.1),
        Question("Who played Sherlock Holmes in the BBC series?", listOf("Robert Downey Jr", "Benedict Cumberbatch", "Jonny Lee Miller", "Henry Cavill"), 1, 0.2),
        Question("What is the name of the coffee shop in Friends?", listOf("The Brew", "Central Perk", "Java Jones", "Coffee Bean"), 1, 0.1),
        Question("Which animated show features a family in Springfield?", listOf("Family Guy", "The Simpsons", "Bob's Burgers", "King of the Hill"), 1, 0.1),
        Question("Who created the TV show 'The Office'?", listOf("Ricky Gervais", "Tina Fey", "Larry David", "Seth MacFarlane"), 0, 0.3),
        Question("Which show takes place in Dunder Mifflin Paper Company?", listOf("Parks and Recreation", "The Office", "Brooklyn Nine-Nine", "30 Rock"), 1, 0.1),
        Question("What year did Netflix begin streaming?", listOf("2005", "2007", "2010", "2012"), 1, 0.4)
    )

    // ── Tech (15 questions) ───────────────────────────────────────────────────
    private val tech = listOf(
        Question("Who founded Apple alongside Steve Wozniak?", listOf("Bill Gates", "Steve Jobs", "Elon Musk", "Jeff Bezos"), 1, 0.1),
        Question("What does 'CPU' stand for?", listOf("Computer Processing Unit", "Central Processing Unit", "Core Processing Unit", "Central Program Unit"), 1, 0.2),
        Question("Which language is primarily used for web development?", listOf("Python", "C++", "JavaScript", "Java"), 2, 0.2),
        Question("What does 'HTML' stand for?", listOf("HyperText Markup Language", "High Tech Modern Language", "HyperText Machine Language", "Home Tool Markup Language"), 0, 0.2),
        Question("Who is the CEO of Tesla?", listOf("Jeff Bezos", "Tim Cook", "Elon Musk", "Sundar Pichai"), 2, 0.1),
        Question("What year was the first iPhone released?", listOf("2005", "2006", "2007", "2008"), 2, 0.2),
        Question("What does 'AI' stand for?", listOf("Advanced Intelligence", "Artificial Intelligence", "Automated Intelligence", "Applied Intelligence"), 1, 0.1),
        Question("Which company created Android?", listOf("Apple", "Microsoft", "Google", "Samsung"), 2, 0.1),
        Question("What is the most popular programming language (2024)?", listOf("Java", "C++", "Python", "JavaScript"), 2, 0.3),
        Question("What does 'URL' stand for?", listOf("Uniform Resource Locator", "Universal Reference Link", "Unified Resource Link", "Universal Resource Locator"), 0, 0.3),
        Question("Which company owns Instagram?", listOf("Google", "Twitter", "Meta (Facebook)", "Snapchat"), 2, 0.1),
        Question("What is the binary for the number 10?", listOf("1010", "1100", "1001", "1110"), 0, 0.4),
        Question("Who founded Microsoft?", listOf("Steve Jobs", "Bill Gates", "Mark Zuckerberg", "Larry Page"), 1, 0.1),
        Question("What does 'RAM' stand for?", listOf("Random Access Memory", "Read Access Memory", "Rapid Access Module", "Random Application Memory"), 0, 0.2),
        Question("Which search engine was created by Larry Page and Sergey Brin?", listOf("Yahoo", "Bing", "Google", "DuckDuckGo"), 2, 0.1)
    )

    // ── Art (15 questions) ────────────────────────────────────────────────────
    private val art = listOf(
        Question("Who painted 'Starry Night'?", listOf("Claude Monet", "Vincent van Gogh", "Pablo Picasso", "Salvador Dalí"), 1, 0.1),
        Question("Which art movement is Salvador Dalí associated with?", listOf("Impressionism", "Cubism", "Surrealism", "Pop Art"), 2, 0.2),
        Question("What material is the Statue of David made from?", listOf("Bronze", "Marble", "Granite", "Limestone"), 1, 0.3),
        Question("Which artist created 'The Persistence of Memory'?", listOf("Picasso", "Dalí", "Warhol", "Monet"), 1, 0.3),
        Question("What are the three primary colors?", listOf("Red, Green, Blue", "Red, Yellow, Blue", "Red, Orange, Purple", "Yellow, Green, Blue"), 1, 0.1),
        Question("Who sculpted 'The Thinker'?", listOf("Michelangelo", "Donatello", "Rodin", "Bernini"), 2, 0.3),
        Question("Which museum houses the Mona Lisa?", listOf("Uffizi Gallery", "British Museum", "Louvre", "Metropolitan Museum"), 2, 0.2),
        Question("What style is Frida Kahlo known for?", listOf("Abstract", "Surrealism", "Impressionism", "Realism"), 1, 0.3),
        Question("Which Dutch artist is famous for 'Girl with a Pearl Earring'?", listOf("Rembrandt", "Vermeer", "Van Gogh", "Mondrian"), 1, 0.3),
        Question("What is the Japanese art of paper folding called?", listOf("Ikebana", "Origami", "Bonsai", "Calligraphy"), 1, 0.1),
        Question("What technique uses small dots to create an image?", listOf("Impressionism", "Pointillism", "Cubism", "Fauvism"), 1, 0.3),
        Question("Who is best known for 'Campbell's Soup Cans'?", listOf("Roy Lichtenstein", "Andy Warhol", "Jasper Johns", "Keith Haring"), 1, 0.2),
        Question("What color do you get by mixing blue and yellow?", listOf("Purple", "Orange", "Green", "Brown"), 2, 0.1),
        Question("Which is a famous art gallery in New York?", listOf("Louvre", "Tate Modern", "MoMA", "Hermitage"), 2, 0.2),
        Question("What art period came after the Middle Ages?", listOf("Baroque", "Renaissance", "Romanticism", "Neoclassicism"), 1, 0.3)
    )

    // ── Literature (15 questions) ─────────────────────────────────────────────
    private val literature = listOf(
        Question("Who wrote 'Romeo and Juliet'?", listOf("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"), 1, 0.1),
        Question("What is the first book of the Harry Potter series?", listOf("Chamber of Secrets", "Philosopher's Stone", "Prisoner of Azkaban", "Goblet of Fire"), 1, 0.1),
        Question("Who wrote '1984'?", listOf("Aldous Huxley", "George Orwell", "Ray Bradbury", "H.G. Wells"), 1, 0.2),
        Question("Which novel features the character 'Jay Gatsby'?", listOf("Catcher in the Rye", "The Great Gatsby", "To Kill a Mockingbird", "Of Mice and Men"), 1, 0.2),
        Question("Who wrote 'Pride and Prejudice'?", listOf("Emily Brontë", "Jane Austen", "Virginia Woolf", "Mary Shelley"), 1, 0.2),
        Question("What is the name of the hobbit in 'The Lord of the Rings'?", listOf("Bilbo", "Frodo", "Sam", "Pippin"), 1, 0.1),
        Question("Who is the author of 'To Kill a Mockingbird'?", listOf("Harper Lee", "F. Scott Fitzgerald", "Ernest Hemingway", "John Steinbeck"), 0, 0.2),
        Question("Which Shakespeare play features the character Hamlet?", listOf("Macbeth", "Hamlet", "Othello", "King Lear"), 1, 0.1),
        Question("Who wrote 'The Alchemist'?", listOf("Gabriel Garcia Marquez", "Paulo Coelho", "Jorge Luis Borges", "Isabel Allende"), 1, 0.2),
        Question("What genre is 'Dracula' by Bram Stoker?", listOf("Romance", "Sci-Fi", "Gothic Horror", "Mystery"), 2, 0.2),
        Question("Who wrote 'A Tale of Two Cities'?", listOf("Thomas Hardy", "Charles Dickens", "Oscar Wilde", "H.G. Wells"), 1, 0.2),
        Question("Which book begins with 'Call me Ishmael'?", listOf("Moby Dick", "The Old Man and the Sea", "Treasure Island", "Robinson Crusoe"), 0, 0.3),
        Question("Who wrote 'The Jungle Book'?", listOf("Mark Twain", "Rudyard Kipling", "Roald Dahl", "Lewis Carroll"), 1, 0.2),
        Question("Which detective was created by Arthur Conan Doyle?", listOf("Hercule Poirot", "Sherlock Holmes", "Philip Marlowe", "Sam Spade"), 1, 0.1),
        Question("What is the pen name of Samuel Clemens?", listOf("O. Henry", "Mark Twain", "Lewis Carroll", "George Eliot"), 1, 0.3)
    )

    // ── Pop Culture (15 questions) ────────────────────────────────────────────
    private val popCulture = listOf(
        Question("Which video game features a plumber named Mario?", listOf("Sonic", "Super Mario Bros", "Zelda", "Pac-Man"), 1, 0.1),
        Question("What social media platform uses a bird logo?", listOf("Facebook", "Instagram", "Twitter/X", "TikTok"), 2, 0.1),
        Question("Who is the most-followed person on Instagram (2024)?", listOf("Kylie Jenner", "Selena Gomez", "Cristiano Ronaldo", "Dwayne Johnson"), 2, 0.3),
        Question("What does 'GG' mean in gaming?", listOf("Great Going", "Good Game", "Get Going", "Game Glitch"), 1, 0.1),
        Question("Which platform is known for short-form videos?", listOf("YouTube", "Facebook", "TikTok", "LinkedIn"), 2, 0.1),
        Question("In Minecraft, what material is needed to make a pickaxe?", listOf("Iron and Sticks", "Wood and Sticks", "Stone and Sticks", "All of the above"), 3, 0.2),
        Question("What is the best-selling video game of all time?", listOf("GTA V", "Minecraft", "Tetris", "Wii Sports"), 1, 0.3),
        Question("Which franchise features Pikachu?", listOf("Digimon", "Pokémon", "Yu-Gi-Oh", "Beyblade"), 1, 0.1),
        Question("What does 'GOAT' stand for in internet culture?", listOf("Got Only A Trick", "Greatest Of All Time", "Go On A Trip", "Great Online Achievement"), 1, 0.1),
        Question("Which battle royale game was developed by Epic Games?", listOf("PUBG", "Apex Legends", "Fortnite", "Warzone"), 2, 0.1),
        Question("What year was YouTube founded?", listOf("2003", "2005", "2007", "2009"), 1, 0.3),
        Question("Who created the Marvel Cinematic Universe's first film?", listOf("Marvel Studios", "DC Comics", "Sony Pictures", "20th Century Fox"), 0, 0.2),
        Question("What anime features a character named Goku?", listOf("Naruto", "One Piece", "Dragon Ball", "Bleach"), 2, 0.1),
        Question("Which game lets you build and survive in a blocky world?", listOf("Roblox", "Minecraft", "Terraria", "Fortnite"), 1, 0.1),
        Question("What does 'meme' originally refer to?", listOf("A joke", "A viral image", "A cultural idea unit", "An internet trend"), 2, 0.4)
    )

    // ── IPL (15 questions) ─────────────────────────────────────────────────────
    private val ipl = listOf(
        Question("Which team won the first IPL season in 2008?", listOf("Chennai Super Kings", "Rajasthan Royals", "Mumbai Indians", "Kolkata Knight Riders"), 1, 0.2),
        Question("Who is the all-time leading run scorer in IPL history?", listOf("Rohit Sharma", "Virat Kohli", "Suresh Raina", "David Warner"), 1, 0.3),
        Question("Which bowler has taken the most wickets in IPL?", listOf("Lasith Malinga", "Yuzvendra Chahal", "Amit Mishra", "Dwayne Bravo"), 1, 0.3),
        Question("How many teams participated in the first IPL season?", listOf("6", "8", "10", "12"), 1, 0.1),
        Question("Which team has won the most IPL titles?", listOf("Chennai Super Kings", "Mumbai Indians", "Kolkata Knight Riders", "Royal Challengers Bengaluru"), 1, 0.2),
        Question("Who hit the first century in IPL history?", listOf("Sachin Tendulkar", "Brendon McCullum", "Chris Gayle", "AB de Villiers"), 1, 0.3),
        Question("In which year was IPL founded?", listOf("2006", "2007", "2008", "2009"), 2, 0.1),
        Question("Which stadium is the home ground of Mumbai Indians?", listOf("Eden Gardens", "Wankhede Stadium", "Chinnaswamy Stadium", "MA Chidambaram Stadium"), 1, 0.2),
        Question("Who captained CSK in their first IPL title win?", listOf("Suresh Raina", "MS Dhoni", "Matthew Hayden", "Stephen Fleming"), 1, 0.1),
        Question("What is the highest individual score in an IPL innings?", listOf("158*", "175*", "183", "140"), 1, 0.4),
        Question("Which franchise was dissolved after IPL 2013?", listOf("Pune Warriors", "Kochi Tuskers", "Deccan Chargers", "Gujarat Lions"), 2, 0.4),
        Question("Who is known as the 'Universe Boss' in IPL?", listOf("AB de Villiers", "Chris Gayle", "Andre Russell", "Kieron Pollard"), 1, 0.1),
        Question("Which IPL team is based in Kolkata?", listOf("Royal Challengers", "Sunrisers", "Knight Riders", "Titans"), 2, 0.1),
        Question("How many overs are played per innings in an IPL match?", listOf("15", "20", "25", "50"), 1, 0.1),
        Question("Who won the Orange Cap in IPL 2023?", listOf("Shubman Gill", "Faf du Plessis", "Virat Kohli", "Devon Conway"), 1, 0.4)
    )
}
