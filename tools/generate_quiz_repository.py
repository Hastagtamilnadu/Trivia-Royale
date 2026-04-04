from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import random
import re
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "android" / "app" / "src" / "main" / "java" / "com" / "triviaroyale" / "data" / "QuizRepository.kt"
BANKS_OUTPUT = ROOT / "android" / "app" / "src" / "main" / "java" / "com" / "triviaroyale" / "data" / "QuizQuestionBanks.kt"


@dataclass(frozen=True)
class Entry:
    name: str
    prop_a: str
    prop_b: str


@dataclass(frozen=True)
class CategorySpec:
    genre: str
    category: str
    entity_label: str
    prop_a_label: str
    prop_b_label: str
    entries: tuple[Entry, ...]


def build_entries(*rows: tuple[str, str, str]) -> tuple[Entry, ...]:
    return tuple(Entry(*row) for row in rows)


COUNTRY_ROWS = (
    ("Japan", "Tokyo", "Asia", "Japanese yen", "red sun disc", "red and white"),
    ("France", "Paris", "Europe", "euro", "blue-white-red tricolor", "blue, white and red"),
    ("Brazil", "Brasilia", "South America", "Brazilian real", "yellow diamond on green", "green, yellow and blue"),
    ("Canada", "Ottawa", "North America", "Canadian dollar", "maple leaf", "red and white"),
    ("Australia", "Canberra", "Oceania", "Australian dollar", "Southern Cross stars", "blue, white and red"),
    ("India", "New Delhi", "Asia", "Indian rupee", "Ashoka Chakra", "saffron, white and green"),
    ("Egypt", "Cairo", "Africa", "Egyptian pound", "Eagle of Saladin", "red, white and black"),
    ("Mexico", "Mexico City", "North America", "Mexican peso", "eagle with serpent", "green, white and red"),
    ("Italy", "Rome", "Europe", "euro", "green-white-red tricolor", "green, white and red"),
    ("South Korea", "Seoul", "Asia", "South Korean won", "taegeuk symbol", "white, red and blue"),
    ("Argentina", "Buenos Aires", "South America", "Argentine peso", "Sun of May", "sky blue and white"),
    ("South Africa", "Pretoria", "Africa", "South African rand", "green Y shape", "red, blue, green, black, white and yellow"),
    ("Spain", "Madrid", "Europe", "euro", "royal coat of arms", "red and yellow"),
    ("Germany", "Berlin", "Europe", "euro", "black-red-gold tricolor", "black, red and gold"),
    ("Kenya", "Nairobi", "Africa", "Kenyan shilling", "Masai shield", "black, red, green and white"),
    ("Thailand", "Bangkok", "Asia", "Thai baht", "five horizontal stripes", "red, white and blue"),
    ("United States", "Washington, D.C.", "North America", "United States dollar", "stars and stripes", "red, white and blue"),
    ("United Kingdom", "London", "Europe", "pound sterling", "Union Jack", "red, white and blue"),
    ("Turkey", "Ankara", "Asia", "Turkish lira", "crescent and star", "red and white"),
    ("Nigeria", "Abuja", "Africa", "naira", "green-white-green bands", "green and white"),
    ("Peru", "Lima", "South America", "sol", "vertical red-white-red bands", "red and white"),
    ("Saudi Arabia", "Riyadh", "Asia", "Saudi riyal", "sword and shahada", "green and white"),
    ("Sweden", "Stockholm", "Europe", "Swedish krona", "Nordic cross", "blue and yellow"),
    ("New Zealand", "Wellington", "Oceania", "New Zealand dollar", "Southern Cross stars", "blue, red and white"),
)

CAPITAL_COUNTRIES = tuple(Entry(country, capital, continent) for country, capital, continent, _, _, _ in COUNTRY_ROWS)
CARTOGRAPHY_COUNTRIES = tuple(Entry(country, continent, capital) for country, capital, continent, _, _, _ in COUNTRY_ROWS)
CURRENCY_COUNTRIES = tuple(Entry(country, currency, capital) for country, capital, _, currency, _, _ in COUNTRY_ROWS)
FLAG_COUNTRIES = tuple(Entry(country, motif, colors) for country, _, _, _, motif, colors in COUNTRY_ROWS)

LANDMARK_ROWS = (
    ("Eiffel Tower", "France", "Paris"),
    ("Statue of Liberty", "United States", "New York City"),
    ("Christ the Redeemer", "Brazil", "Rio de Janeiro"),
    ("Colosseum", "Italy", "Rome"),
    ("Great Pyramid of Giza", "Egypt", "Giza"),
    ("Sydney Opera House", "Australia", "Sydney"),
    ("Taj Mahal", "India", "Agra"),
    ("Machu Picchu", "Peru", "Cusco Region"),
    ("Big Ben", "United Kingdom", "London"),
    ("Sagrada Familia", "Spain", "Barcelona"),
    ("Mount Fuji", "Japan", "Honshu"),
    ("Burj Khalifa", "United Arab Emirates", "Dubai"),
    ("Table Mountain", "South Africa", "Cape Town"),
    ("Golden Gate Bridge", "United States", "San Francisco"),
    ("Angkor Wat", "Cambodia", "Siem Reap"),
    ("CN Tower", "Canada", "Toronto"),
    ("Petra", "Jordan", "Ma'an Governorate"),
    ("Leaning Tower of Pisa", "Italy", "Pisa"),
    ("Moai of Easter Island", "Chile", "Rapa Nui"),
    ("Acropolis of Athens", "Greece", "Athens"),
    ("Brandenburg Gate", "Germany", "Berlin"),
)

LANDMARKS = build_entries(*LANDMARK_ROWS)

INVENTION_ROWS = (
    ("telephone", "Alexander Graham Bell", "1876"),
    ("light bulb", "Thomas Edison", "1879"),
    ("printing press", "Johannes Gutenberg", "1450"),
    ("airplane", "Wright brothers", "1903"),
    ("World Wide Web", "Tim Berners-Lee", "1989"),
    ("steam engine", "James Watt", "1769"),
    ("television", "John Logie Baird", "1926"),
    ("radio", "Guglielmo Marconi", "1895"),
    ("vaccination", "Edward Jenner", "1796"),
    ("dynamite", "Alfred Nobel", "1867"),
    ("computer mouse", "Douglas Engelbart", "1964"),
    ("refrigerator", "Carl von Linde", "1876"),
    ("diesel engine", "Rudolf Diesel", "1893"),
    ("helicopter", "Igor Sikorsky", "1939"),
    ("ballpoint pen", "Laszlo Biro", "1938"),
    ("microwave oven", "Percy Spencer", "1945"),
    ("sewing machine", "Elias Howe", "1846"),
    ("phonograph", "Thomas Edison", "1877"),
    ("periodic table", "Dmitri Mendeleev", "1869"),
    ("zipper", "Gideon Sundback", "1913"),
    ("instant camera", "Edwin Land", "1948"),
)

INVENTIONS = build_entries(*INVENTION_ROWS)

ELEMENT_ROWS = (
    ("Hydrogen", "H", "1"),
    ("Helium", "He", "2"),
    ("Lithium", "Li", "3"),
    ("Carbon", "C", "6"),
    ("Nitrogen", "N", "7"),
    ("Oxygen", "O", "8"),
    ("Sodium", "Na", "11"),
    ("Magnesium", "Mg", "12"),
    ("Aluminum", "Al", "13"),
    ("Silicon", "Si", "14"),
    ("Phosphorus", "P", "15"),
    ("Sulfur", "S", "16"),
    ("Chlorine", "Cl", "17"),
    ("Potassium", "K", "19"),
    ("Calcium", "Ca", "20"),
    ("Iron", "Fe", "26"),
    ("Copper", "Cu", "29"),
    ("Silver", "Ag", "47"),
    ("Tin", "Sn", "50"),
    ("Gold", "Au", "79"),
    ("Lead", "Pb", "82"),
)

ELEMENTS = build_entries(*ELEMENT_ROWS)

MYTH_ROWS = (
    ("Zeus", "Greek", "sky and thunder"),
    ("Hera", "Greek", "marriage"),
    ("Poseidon", "Greek", "sea"),
    ("Athena", "Greek", "wisdom"),
    ("Ares", "Greek", "war"),
    ("Apollo", "Greek", "sun and music"),
    ("Artemis", "Greek", "hunt"),
    ("Odin", "Norse", "wisdom and kingship"),
    ("Thor", "Norse", "thunder"),
    ("Loki", "Norse", "trickery"),
    ("Freya", "Norse", "love"),
    ("Anubis", "Egyptian", "mummification"),
    ("Ra", "Egyptian", "sun"),
    ("Isis", "Egyptian", "magic"),
    ("Horus", "Egyptian", "sky"),
    ("Osiris", "Egyptian", "afterlife"),
    ("Ganesha", "Hindu", "beginnings"),
    ("Lakshmi", "Hindu", "wealth"),
    ("Saraswati", "Hindu", "learning"),
    ("Hanuman", "Hindu", "devotion"),
    ("Kali", "Hindu", "destruction and time"),
)

MYTH_FIGURES = build_entries(*MYTH_ROWS)

BOOK_ROWS = (
    ("Pride and Prejudice", "Jane Austen", "1813", "Elizabeth Bennet"),
    ("Moby-Dick", "Herman Melville", "1851", "Captain Ahab"),
    ("Jane Eyre", "Charlotte Bronte", "1847", "Jane Eyre"),
    ("Wuthering Heights", "Emily Bronte", "1847", "Heathcliff"),
    ("Great Expectations", "Charles Dickens", "1861", "Pip"),
    ("Crime and Punishment", "Fyodor Dostoevsky", "1866", "Raskolnikov"),
    ("War and Peace", "Leo Tolstoy", "1869", "Pierre Bezukhov"),
    ("The Adventures of Huckleberry Finn", "Mark Twain", "1884", "Huckleberry Finn"),
    ("The Picture of Dorian Gray", "Oscar Wilde", "1890", "Dorian Gray"),
    ("The Great Gatsby", "F. Scott Fitzgerald", "1925", "Jay Gatsby"),
    ("1984", "George Orwell", "1949", "Winston Smith"),
    ("The Catcher in the Rye", "J. D. Salinger", "1951", "Holden Caulfield"),
    ("Fahrenheit 451", "Ray Bradbury", "1953", "Guy Montag"),
    ("To Kill a Mockingbird", "Harper Lee", "1960", "Scout Finch"),
    ("One Hundred Years of Solitude", "Gabriel Garcia Marquez", "1967", "Aureliano Buendia"),
    ("Beloved", "Toni Morrison", "1987", "Sethe"),
    ("The Kite Runner", "Khaled Hosseini", "2003", "Amir"),
    ("Life of Pi", "Yann Martel", "2001", "Pi Patel"),
    ("The Book Thief", "Markus Zusak", "2005", "Liesel Meminger"),
    ("The Night Circus", "Erin Morgenstern", "2011", "Celia Bowen"),
    ("The Goldfinch", "Donna Tartt", "2013", "Theo Decker"),
    ("A Gentleman in Moscow", "Amor Towles", "2016", "Count Rostov"),
    ("Where the Crawdads Sing", "Delia Owens", "2018", "Kya Clark"),
    ("Klara and the Sun", "Kazuo Ishiguro", "2021", "Klara"),
)

CLASSIC_BOOKS = tuple(Entry(title, author, year) for title, author, year, _ in BOOK_ROWS[:21])
MODERN_BOOKS = tuple(Entry(title, author, year) for title, author, year, _ in BOOK_ROWS[3:24])
BOOK_WORLDS = tuple(Entry(character, title, author) for title, author, _, character in BOOK_ROWS[:21])

POEM_ROWS = (
    ("The Raven", "Edgar Allan Poe", "1845"),
    ("If", "Rudyard Kipling", "1910"),
    ("Ozymandias", "Percy Bysshe Shelley", "1818"),
    ("Daffodils", "William Wordsworth", "1807"),
    ("Invictus", "William Ernest Henley", "1875"),
    ("Do Not Go Gentle into That Good Night", "Dylan Thomas", "1951"),
    ("Still I Rise", "Maya Angelou", "1978"),
    ("Stopping by Woods on a Snowy Evening", "Robert Frost", "1923"),
    ("Annabel Lee", "Edgar Allan Poe", "1849"),
    ("The Road Not Taken", "Robert Frost", "1916"),
    ("Phenomenal Woman", "Maya Angelou", "1978"),
    ("Kubla Khan", "Samuel Taylor Coleridge", "1816"),
    ("How Do I Love Thee", "Elizabeth Barrett Browning", "1850"),
    ("Ulysses", "Alfred, Lord Tennyson", "1842"),
    ("We Real Cool", "Gwendolyn Brooks", "1960"),
    ("Fire and Ice", "Robert Frost", "1920"),
    ("A Psalm of Life", "Henry Wadsworth Longfellow", "1838"),
    ("The Charge of the Light Brigade", "Alfred, Lord Tennyson", "1854"),
    ("Lady Lazarus", "Sylvia Plath", "1965"),
    ("Daddy", "Sylvia Plath", "1965"),
    ("Still Falls the Rain", "Edith Sitwell", "1941"),
)

POEMS = build_entries(*POEM_ROWS)

SONG_ROWS = (
    ("Bohemian Rhapsody", "Queen", "1975", "A Night at the Opera"),
    ("Billie Jean", "Michael Jackson", "1982", "Thriller"),
    ("Like a Prayer", "Madonna", "1989", "Like a Prayer"),
    ("Smells Like Teen Spirit", "Nirvana", "1991", "Nevermind"),
    ("Wonderwall", "Oasis", "1995", "(What's the Story) Morning Glory?"),
    ("Hey Ya!", "Outkast", "2003", "Speakerboxxx/The Love Below"),
    ("Crazy in Love", "Beyonce", "2003", "Dangerously in Love"),
    ("Viva La Vida", "Coldplay", "2008", "Viva la Vida or Death and All His Friends"),
    ("Rolling in the Deep", "Adele", "2010", "21"),
    ("Uptown Funk", "Mark Ronson", "2014", "Uptown Special"),
    ("Shape of You", "Ed Sheeran", "2017", "Divide"),
    ("Blinding Lights", "The Weeknd", "2019", "After Hours"),
    ("Levitating", "Dua Lipa", "2020", "Future Nostalgia"),
    ("Shake It Off", "Taylor Swift", "2014", "1989"),
    ("Hotel California", "Eagles", "1976", "Hotel California"),
    ("Imagine", "John Lennon", "1971", "Imagine"),
    ("Respect", "Aretha Franklin", "1967", "I Never Loved a Man the Way I Love You"),
    ("Purple Rain", "Prince", "1984", "Purple Rain"),
    ("Born to Run", "Bruce Springsteen", "1975", "Born to Run"),
    ("Poker Face", "Lady Gaga", "2008", "The Fame"),
    ("Halo", "Beyonce", "2008", "I Am... Sasha Fierce"),
    ("Someone Like You", "Adele", "2011", "21"),
    ("Bad Guy", "Billie Eilish", "2019", "When We All Fall Asleep, Where Do We Go?"),
    ("As It Was", "Harry Styles", "2022", "Harry's House"),
)

CHART_SONGS = tuple(Entry(title, artist, year) for title, artist, year, _ in SONG_ROWS[:21])
NAME_THAT_ARTIST = tuple(Entry(title, artist, album) for title, artist, _, album in SONG_ROWS[3:24])

ALBUM_ROWS = (
    ("Thriller", "Michael Jackson", "1982", "pop"),
    ("Back in Black", "AC/DC", "1980", "rock"),
    ("The Dark Side of the Moon", "Pink Floyd", "1973", "progressive rock"),
    ("Rumours", "Fleetwood Mac", "1977", "rock"),
    ("Abbey Road", "The Beatles", "1969", "rock"),
    ("Purple Rain", "Prince", "1984", "pop rock"),
    ("Nevermind", "Nirvana", "1991", "grunge"),
    ("21", "Adele", "2011", "soul pop"),
    ("The Miseducation of Lauryn Hill", "Lauryn Hill", "1998", "R&B"),
    ("Kind of Blue", "Miles Davis", "1959", "jazz"),
    ("Blue", "Joni Mitchell", "1971", "folk"),
    ("Lemonade", "Beyonce", "2016", "R&B"),
    ("1989", "Taylor Swift", "2014", "synth-pop"),
    ("Future Nostalgia", "Dua Lipa", "2020", "dance-pop"),
    ("Born to Die", "Lana Del Rey", "2012", "baroque pop"),
    ("Sgt. Pepper's Lonely Hearts Club Band", "The Beatles", "1967", "psychedelic rock"),
    ("The Joshua Tree", "U2", "1987", "rock"),
    ("Good Kid, M.A.A.D City", "Kendrick Lamar", "2012", "hip-hop"),
    ("After Hours", "The Weeknd", "2020", "synth-pop"),
    ("Jagged Little Pill", "Alanis Morissette", "1995", "alternative rock"),
    ("Tapestry", "Carole King", "1971", "singer-songwriter"),
)

CLASSIC_ALBUMS = tuple(Entry(album, artist, year) for album, artist, year, _ in ALBUM_ROWS)

ARTIST_ROWS = (
    ("Taylor Swift", "United States", "pop"),
    ("BTS", "South Korea", "K-pop"),
    ("Shakira", "Colombia", "Latin pop"),
    ("Adele", "United Kingdom", "soul pop"),
    ("Drake", "Canada", "hip-hop"),
    ("Bad Bunny", "Puerto Rico", "reggaeton"),
    ("Rema", "Nigeria", "Afrobeats"),
    ("Ed Sheeran", "United Kingdom", "pop"),
    ("Rosalia", "Spain", "flamenco pop"),
    ("A. R. Rahman", "India", "film score"),
    ("Celine Dion", "Canada", "pop"),
    ("Rihanna", "Barbados", "pop"),
    ("Sia", "Australia", "pop"),
    ("ABBA", "Sweden", "pop"),
    ("Fela Kuti", "Nigeria", "Afrobeat"),
    ("Andrea Bocelli", "Italy", "classical crossover"),
    ("Juanes", "Colombia", "Latin rock"),
    ("Youssou N'Dour", "Senegal", "mbalax"),
    ("Enya", "Ireland", "new age"),
    ("Anitta", "Brazil", "pop"),
    ("Dua Lipa", "United Kingdom", "dance-pop"),
)

GLOBAL_ARTISTS = build_entries(*ARTIST_ROWS)

COMPANY_FACT_ROWS = (
    ("Apple", "1976", "United States"),
    ("Microsoft", "1975", "United States"),
    ("Google", "1998", "United States"),
    ("Amazon", "1994", "United States"),
    ("Meta", "2004", "United States"),
    ("Netflix", "1997", "United States"),
    ("Tesla", "2003", "United States"),
    ("Spotify", "2006", "Sweden"),
    ("Airbnb", "2008", "United States"),
    ("Uber", "2009", "United States"),
    ("Adobe", "1982", "United States"),
    ("Intel", "1968", "United States"),
    ("Nvidia", "1993", "United States"),
    ("Samsung", "1938", "South Korea"),
    ("Sony", "1946", "Japan"),
    ("PayPal", "1998", "United States"),
    ("SpaceX", "2002", "United States"),
    ("Zoom", "2011", "United States"),
    ("OpenAI", "2015", "United States"),
    ("Slack", "2013", "United States"),
    ("Dropbox", "2007", "United States"),
)

STARTUPS = build_entries(*COMPANY_FACT_ROWS)
BIG_TECH = tuple(Entry(company, country, year) for company, year, country in COMPANY_FACT_ROWS)

LANGUAGE_ROWS = (
    ("Python", "Guido van Rossum", "multi-paradigm"),
    ("Java", "James Gosling", "object-oriented"),
    ("C", "Dennis Ritchie", "procedural"),
    ("C++", "Bjarne Stroustrup", "multi-paradigm"),
    ("JavaScript", "Brendan Eich", "multi-paradigm"),
    ("TypeScript", "Anders Hejlsberg", "typed superset"),
    ("Go", "Robert Griesemer", "compiled concurrency"),
    ("Rust", "Graydon Hoare", "systems programming"),
    ("Kotlin", "JetBrains", "JVM-first"),
    ("Swift", "Apple", "Apple platform"),
    ("Ruby", "Yukihiro Matsumoto", "object-oriented"),
    ("PHP", "Rasmus Lerdorf", "server-side scripting"),
    ("C#", "Anders Hejlsberg", "object-oriented"),
    ("Scala", "Martin Odersky", "functional and object-oriented"),
    ("R", "Ross Ihaka", "statistical computing"),
    ("Julia", "Jeff Bezanson", "numerical computing"),
    ("Haskell", "Simon Peyton Jones", "purely functional"),
    ("Perl", "Larry Wall", "text processing"),
    ("Dart", "Lars Bak", "UI development"),
    ("Lua", "Roberto Ierusalimschy", "embeddable scripting"),
    ("Elixir", "Jose Valim", "functional concurrency"),
)

LANGUAGES = build_entries(*LANGUAGE_ROWS)

TECH_TREND_ROWS = (
    ("AI", "artificial intelligence", "machine learning systems"),
    ("AR", "augmented reality", "digital overlays on live views"),
    ("VR", "virtual reality", "immersive simulated environments"),
    ("IoT", "internet of things", "networked smart devices"),
    ("5G", "fifth generation", "mobile connectivity"),
    ("SaaS", "software as a service", "cloud-delivered software"),
    ("PaaS", "platform as a service", "managed deployment platform"),
    ("IaaS", "infrastructure as a service", "cloud compute resources"),
    ("API", "application programming interface", "software integration"),
    ("NLP", "natural language processing", "language understanding"),
    ("GPU", "graphics processing unit", "parallel computation"),
    ("CPU", "central processing unit", "general instruction execution"),
    ("OCR", "optical character recognition", "text extraction from images"),
    ("RPA", "robotic process automation", "workflow automation"),
    ("MLOps", "machine learning operations", "model deployment and monitoring"),
    ("Edge computing", "local processing", "compute near data source"),
    ("Quantum computing", "qubits", "probabilistic computation"),
    ("Digital twin", "virtual replica", "simulation of real systems"),
    ("Zero trust", "verify every request", "security architecture"),
    ("Prompt engineering", "instruction design", "LLM task guidance"),
    ("Computer vision", "image understanding", "machine perception"),
)

FUTURE_TECH = build_entries(*TECH_TREND_ROWS)

TV_ROWS = (
    ("Stranger Things", "Netflix", "2016", "Eleven"),
    ("Breaking Bad", "AMC", "2008", "Walter White"),
    ("Game of Thrones", "HBO", "2011", "Jon Snow"),
    ("Friends", "NBC", "1994", "Rachel Green"),
    ("The Office", "NBC", "2005", "Michael Scott"),
    ("The Crown", "Netflix", "2016", "Queen Elizabeth II"),
    ("The Mandalorian", "Disney+", "2019", "Din Djarin"),
    ("Sherlock", "BBC", "2010", "Sherlock Holmes"),
    ("Wednesday", "Netflix", "2022", "Wednesday Addams"),
    ("The Boys", "Prime Video", "2019", "Homelander"),
    ("The Last of Us", "HBO", "2023", "Joel Miller"),
    ("Bridgerton", "Netflix", "2020", "Daphne Bridgerton"),
    ("Money Heist", "Antena 3", "2017", "The Professor"),
    ("Squid Game", "Netflix", "2021", "Seong Gi-hun"),
    ("The Bear", "FX", "2022", "Carmen Berzatto"),
    ("Succession", "HBO", "2018", "Logan Roy"),
    ("House of the Dragon", "HBO", "2022", "Rhaenyra Targaryen"),
    ("Only Murders in the Building", "Hulu", "2021", "Charles-Haden Savage"),
    ("Lupin", "Netflix", "2021", "Assane Diop"),
    ("Dark", "Netflix", "2017", "Jonas Kahnwald"),
    ("Ted Lasso", "Apple TV+", "2020", "Ted Lasso"),
)

TV_SHOWS = tuple(Entry(title, platform, year) for title, platform, year, _ in TV_ROWS)
SERIES_CHARACTERS = tuple(Entry(character, title, platform) for title, platform, _, character in TV_ROWS)

STREAMING_ROWS = (
    ("Stranger Things", "Netflix", "science fiction"),
    ("Breaking Bad", "AMC", "crime drama"),
    ("Game of Thrones", "HBO", "fantasy drama"),
    ("Friends", "NBC", "sitcom"),
    ("The Office", "NBC", "workplace comedy"),
    ("The Crown", "Netflix", "historical drama"),
    ("The Mandalorian", "Disney+", "space western"),
    ("Sherlock", "BBC", "detective drama"),
    ("Wednesday", "Netflix", "supernatural comedy"),
    ("The Boys", "Prime Video", "superhero satire"),
    ("The Last of Us", "HBO", "post-apocalyptic drama"),
    ("Bridgerton", "Netflix", "period romance"),
    ("Money Heist", "Antena 3", "heist thriller"),
    ("Squid Game", "Netflix", "survival thriller"),
    ("The Bear", "FX", "comedy drama"),
    ("Succession", "HBO", "satirical drama"),
    ("House of the Dragon", "HBO", "fantasy drama"),
    ("Only Murders in the Building", "Hulu", "mystery comedy"),
    ("Lupin", "Netflix", "heist drama"),
    ("Dark", "Netflix", "science fiction thriller"),
    ("Ted Lasso", "Apple TV+", "sports comedy"),
)

STREAMING_SHOWS = build_entries(*STREAMING_ROWS)

CELEBRITY_ROWS = (
    ("Zendaya", "Rue Bennett", "United States"),
    ("Timothee Chalamet", "Paul Atreides", "United States"),
    ("Emma Stone", "Mia Dolan", "United States"),
    ("Tom Holland", "Spider-Man", "United Kingdom"),
    ("Margot Robbie", "Barbie", "Australia"),
    ("Ryan Gosling", "Ken", "Canada"),
    ("Florence Pugh", "Yelena Belova", "United Kingdom"),
    ("Pedro Pascal", "Joel Miller", "Chile"),
    ("Priyanka Chopra Jonas", "Alex Parrish", "India"),
    ("Chris Hemsworth", "Thor", "Australia"),
    ("Millie Bobby Brown", "Eleven", "United Kingdom"),
    ("Robert Downey Jr.", "Iron Man", "United States"),
    ("Anya Taylor-Joy", "Beth Harmon", "Argentina"),
    ("Ryan Reynolds", "Deadpool", "Canada"),
    ("Jenna Ortega", "Wednesday Addams", "United States"),
    ("Saoirse Ronan", "Lady Bird", "Ireland"),
    ("Dev Patel", "Jamal Malik", "United Kingdom"),
    ("Gal Gadot", "Wonder Woman", "Israel"),
    ("Ke Huy Quan", "Waymond Wang", "Vietnam"),
    ("Michelle Yeoh", "Evelyn Wang", "Malaysia"),
    ("Cillian Murphy", "J. Robert Oppenheimer", "Ireland"),
)

CELEBRITIES = build_entries(*CELEBRITY_ROWS)

GAME_ROWS = (
    ("Minecraft", "Mojang Studios", "2011"),
    ("The Legend of Zelda: Breath of the Wild", "Nintendo", "2017"),
    ("Fortnite", "Epic Games", "2017"),
    ("Grand Theft Auto V", "Rockstar Games", "2013"),
    ("The Witcher 3: Wild Hunt", "CD Projekt", "2015"),
    ("God of War", "Santa Monica Studio", "2018"),
    ("Red Dead Redemption 2", "Rockstar Games", "2018"),
    ("Among Us", "Innersloth", "2018"),
    ("Elden Ring", "FromSoftware", "2022"),
    ("Hades", "Supergiant Games", "2020"),
    ("Overwatch", "Blizzard Entertainment", "2016"),
    ("Valorant", "Riot Games", "2020"),
    ("League of Legends", "Riot Games", "2009"),
    ("Super Mario Odyssey", "Nintendo", "2017"),
    ("The Last of Us Part II", "Naughty Dog", "2020"),
    ("Animal Crossing: New Horizons", "Nintendo", "2020"),
    ("Portal 2", "Valve", "2011"),
    ("Celeste", "Maddy Makes Games", "2018"),
    ("Stardew Valley", "ConcernedApe", "2016"),
    ("Resident Evil 4", "Capcom", "2005"),
    ("Baldur's Gate 3", "Larian Studios", "2023"),
)

GAMES = build_entries(*GAME_ROWS)

FILM_ROWS = (
    ("Titanic", "James Cameron", "1997", "Jack Dawson"),
    ("Inception", "Christopher Nolan", "2010", "Dom Cobb"),
    ("Avatar", "James Cameron", "2009", "Jake Sully"),
    ("The Dark Knight", "Christopher Nolan", "2008", "Batman"),
    ("Jurassic Park", "Steven Spielberg", "1993", "Alan Grant"),
    ("The Matrix", "The Wachowskis", "1999", "Neo"),
    ("Black Panther", "Ryan Coogler", "2018", "T'Challa"),
    ("The Lion King", "Roger Allers and Rob Minkoff", "1994", "Simba"),
    ("Frozen", "Chris Buck and Jennifer Lee", "2013", "Elsa"),
    ("Forrest Gump", "Robert Zemeckis", "1994", "Forrest Gump"),
    ("Interstellar", "Christopher Nolan", "2014", "Cooper"),
    ("Top Gun: Maverick", "Joseph Kosinski", "2022", "Pete Maverick Mitchell"),
    ("The Avengers", "Joss Whedon", "2012", "Iron Man"),
    ("Finding Nemo", "Andrew Stanton and Lee Unkrich", "2003", "Marlin"),
    ("The Shawshank Redemption", "Frank Darabont", "1994", "Andy Dufresne"),
    ("Gladiator", "Ridley Scott", "2000", "Maximus"),
    ("Spider-Man: Into the Spider-Verse", "Bob Persichetti, Peter Ramsey and Rodney Rothman", "2018", "Miles Morales"),
    ("Coco", "Lee Unkrich and Adrian Molina", "2017", "Miguel"),
    ("Mad Max: Fury Road", "George Miller", "2015", "Imperator Furiosa"),
    ("Dune", "Denis Villeneuve", "2021", "Paul Atreides"),
    ("Barbie", "Greta Gerwig", "2023", "Barbie"),
)

BLOCKBUSTERS = tuple(Entry(title, director, year) for title, director, year, _ in FILM_ROWS)
FRANCHISE_CHARACTERS = tuple(Entry(character, title, year) for title, _, year, character in FILM_ROWS)

AWARD_FILM_ROWS = (
    ("The Godfather", "Francis Ford Coppola", "1973"),
    ("The Godfather Part II", "Francis Ford Coppola", "1975"),
    ("Rocky", "John G. Avildsen", "1977"),
    ("Kramer vs. Kramer", "Robert Benton", "1980"),
    ("Gandhi", "Richard Attenborough", "1983"),
    ("Amadeus", "Milos Forman", "1985"),
    ("Platoon", "Oliver Stone", "1987"),
    ("Rain Man", "Barry Levinson", "1989"),
    ("The Silence of the Lambs", "Jonathan Demme", "1992"),
    ("Schindler's List", "Steven Spielberg", "1994"),
    ("Braveheart", "Mel Gibson", "1996"),
    ("Titanic", "James Cameron", "1998"),
    ("Gladiator", "Ridley Scott", "2001"),
    ("The Lord of the Rings: The Return of the King", "Peter Jackson", "2004"),
    ("No Country for Old Men", "Coen brothers", "2008"),
    ("The King's Speech", "Tom Hooper", "2011"),
    ("Argo", "Ben Affleck", "2013"),
    ("Spotlight", "Tom McCarthy", "2016"),
    ("Moonlight", "Barry Jenkins", "2017"),
    ("Parasite", "Bong Joon-ho", "2020"),
    ("Oppenheimer", "Christopher Nolan", "2024"),
)

AWARD_WINNERS = build_entries(*AWARD_FILM_ROWS)

AWARD_SHOW_ROWS = (
    ("Academy Awards", "film", "Los Angeles"),
    ("Golden Globe Awards", "film and television", "Beverly Hills"),
    ("Primetime Emmy Awards", "television", "Los Angeles"),
    ("Grammy Awards", "music", "Los Angeles"),
    ("Tony Awards", "theater", "New York City"),
    ("BAFTA Film Awards", "film", "London"),
    ("Screen Actors Guild Awards", "film and television", "Los Angeles"),
    ("Critics Choice Awards", "film and television", "Los Angeles"),
    ("MTV Video Music Awards", "music videos", "New York City"),
    ("American Music Awards", "music", "Los Angeles"),
    ("BRIT Awards", "music", "London"),
    ("CMA Awards", "country music", "Nashville"),
    ("Billboard Music Awards", "music", "Los Angeles"),
    ("Peabody Awards", "broadcasting and digital media", "New York City"),
    ("Daytime Emmy Awards", "daytime television", "Los Angeles"),
    ("Annie Awards", "animation", "Los Angeles"),
    ("Independent Spirit Awards", "independent film", "Santa Monica"),
    ("People's Choice Awards", "popular culture", "Santa Monica"),
    ("BET Awards", "music and entertainment", "Los Angeles"),
    ("Kids' Choice Awards", "family entertainment", "Los Angeles"),
    ("Cesar Awards", "film", "Paris"),
)

AWARD_SHOWS = build_entries(*AWARD_SHOW_ROWS)

CULT_FILM_ROWS = (
    ("The Big Lebowski", "Coen brothers", "1998"),
    ("Fight Club", "David Fincher", "1999"),
    ("Donnie Darko", "Richard Kelly", "2001"),
    ("Blade Runner", "Ridley Scott", "1982"),
    ("The Princess Bride", "Rob Reiner", "1987"),
    ("Pulp Fiction", "Quentin Tarantino", "1994"),
    ("The Rocky Horror Picture Show", "Jim Sharman", "1975"),
    ("Heathers", "Michael Lehmann", "1988"),
    ("The Grand Budapest Hotel", "Wes Anderson", "2014"),
    ("Pan's Labyrinth", "Guillermo del Toro", "2006"),
    ("Spirited Away", "Hayao Miyazaki", "2001"),
    ("Napoleon Dynamite", "Jared Hess", "2004"),
    ("Ferris Bueller's Day Off", "John Hughes", "1986"),
    ("A Clockwork Orange", "Stanley Kubrick", "1971"),
    ("The Thing", "John Carpenter", "1982"),
    ("Memento", "Christopher Nolan", "2000"),
    ("Scott Pilgrim vs. the World", "Edgar Wright", "2010"),
    ("Eternal Sunshine of the Spotless Mind", "Michel Gondry", "2004"),
    ("The Truman Show", "Peter Weir", "1998"),
    ("Amelie", "Jean-Pierre Jeunet", "2001"),
    ("Moonrise Kingdom", "Wes Anderson", "2012"),
)

CULT_CLASSICS = build_entries(*CULT_FILM_ROWS)

CRICKET_ROWS = (
    ("Sachin Tendulkar", "India", "batter"),
    ("Virat Kohli", "India", "batter"),
    ("M.S. Dhoni", "India", "wicketkeeper"),
    ("Kapil Dev", "India", "all-rounder"),
    ("Wasim Akram", "Pakistan", "fast bowler"),
    ("Waqar Younis", "Pakistan", "fast bowler"),
    ("Imran Khan", "Pakistan", "all-rounder"),
    ("Brian Lara", "West Indies", "batter"),
    ("Viv Richards", "West Indies", "batter"),
    ("Garfield Sobers", "West Indies", "all-rounder"),
    ("Shane Warne", "Australia", "leg spinner"),
    ("Glenn McGrath", "Australia", "fast bowler"),
    ("Ricky Ponting", "Australia", "batter"),
    ("Don Bradman", "Australia", "batter"),
    ("Jacques Kallis", "South Africa", "all-rounder"),
    ("AB de Villiers", "South Africa", "batter"),
    ("Dale Steyn", "South Africa", "fast bowler"),
    ("Ben Stokes", "England", "all-rounder"),
    ("Joe Root", "England", "batter"),
    ("Kumar Sangakkara", "Sri Lanka", "wicketkeeper"),
    ("Muttiah Muralitharan", "Sri Lanka", "off spinner"),
)

CRICKET_LEGENDS = build_entries(*CRICKET_ROWS)

WORLD_CUP_ROWS = (
    ("1930 FIFA World Cup", "Uruguay", "Uruguay"),
    ("1934 FIFA World Cup", "Italy", "Italy"),
    ("1938 FIFA World Cup", "Italy", "France"),
    ("1950 FIFA World Cup", "Uruguay", "Brazil"),
    ("1954 FIFA World Cup", "West Germany", "Switzerland"),
    ("1958 FIFA World Cup", "Brazil", "Sweden"),
    ("1962 FIFA World Cup", "Brazil", "Chile"),
    ("1966 FIFA World Cup", "England", "England"),
    ("1970 FIFA World Cup", "Brazil", "Mexico"),
    ("1974 FIFA World Cup", "West Germany", "West Germany"),
    ("1978 FIFA World Cup", "Argentina", "Argentina"),
    ("1982 FIFA World Cup", "Italy", "Spain"),
    ("1986 FIFA World Cup", "Argentina", "Mexico"),
    ("1990 FIFA World Cup", "West Germany", "Italy"),
    ("1994 FIFA World Cup", "Brazil", "United States"),
    ("1998 FIFA World Cup", "France", "France"),
    ("2002 FIFA World Cup", "Brazil", "South Korea and Japan"),
    ("2006 FIFA World Cup", "Italy", "Germany"),
    ("2010 FIFA World Cup", "Spain", "South Africa"),
    ("2014 FIFA World Cup", "Germany", "Brazil"),
    ("2018 FIFA World Cup", "France", "Russia"),
    ("2022 FIFA World Cup", "Argentina", "Qatar"),
)

WORLD_CUPS = build_entries(*WORLD_CUP_ROWS)

NBA_ROWS = (
    ("Michael Jordan", "shooting guard", "Chicago Bulls"),
    ("LeBron James", "small forward", "Cleveland Cavaliers"),
    ("Kobe Bryant", "shooting guard", "Los Angeles Lakers"),
    ("Magic Johnson", "point guard", "Los Angeles Lakers"),
    ("Larry Bird", "small forward", "Boston Celtics"),
    ("Stephen Curry", "point guard", "Golden State Warriors"),
    ("Shaquille O'Neal", "center", "Los Angeles Lakers"),
    ("Tim Duncan", "power forward", "San Antonio Spurs"),
    ("Kevin Durant", "small forward", "Oklahoma City Thunder"),
    ("Kareem Abdul-Jabbar", "center", "Los Angeles Lakers"),
    ("Wilt Chamberlain", "center", "Philadelphia 76ers"),
    ("Bill Russell", "center", "Boston Celtics"),
    ("Hakeem Olajuwon", "center", "Houston Rockets"),
    ("Dirk Nowitzki", "power forward", "Dallas Mavericks"),
    ("Dwyane Wade", "shooting guard", "Miami Heat"),
    ("Allen Iverson", "point guard", "Philadelphia 76ers"),
    ("Charles Barkley", "power forward", "Phoenix Suns"),
    ("Scottie Pippen", "small forward", "Chicago Bulls"),
    ("Steve Nash", "point guard", "Phoenix Suns"),
    ("Giannis Antetokounmpo", "power forward", "Milwaukee Bucks"),
    ("Kawhi Leonard", "small forward", "San Antonio Spurs"),
)

NBA_ALL_STARS = build_entries(*NBA_ROWS)

TENNIS_ROWS = (
    ("Roger Federer", "Switzerland", "Wimbledon"),
    ("Rafael Nadal", "Spain", "French Open"),
    ("Novak Djokovic", "Serbia", "Australian Open"),
    ("Serena Williams", "United States", "US Open"),
    ("Steffi Graf", "Germany", "Wimbledon"),
    ("Pete Sampras", "United States", "Wimbledon"),
    ("Bjorn Borg", "Sweden", "French Open"),
    ("Martina Navratilova", "United States", "Wimbledon"),
    ("Chris Evert", "United States", "French Open"),
    ("Andre Agassi", "United States", "Australian Open"),
    ("Margaret Court", "Australia", "Australian Open"),
    ("Monica Seles", "Yugoslavia", "Australian Open"),
    ("Billie Jean King", "United States", "Wimbledon"),
    ("John McEnroe", "United States", "US Open"),
    ("Venus Williams", "United States", "Wimbledon"),
    ("Andy Murray", "United Kingdom", "Wimbledon"),
    ("Naomi Osaka", "Japan", "US Open"),
    ("Carlos Alcaraz", "Spain", "Wimbledon"),
    ("Iga Swiatek", "Poland", "French Open"),
    ("Rod Laver", "Australia", "Australian Open"),
    ("Ivan Lendl", "Czech Republic", "US Open"),
)

TENNIS_GRAND_SLAMS = build_entries(*TENNIS_ROWS)

SPACE_ROWS = (
    ("Mercury", "planet", "closest to the Sun"),
    ("Venus", "planet", "second from the Sun"),
    ("Earth", "planet", "third from the Sun"),
    ("Mars", "planet", "red planet"),
    ("Jupiter", "planet", "largest planet"),
    ("Saturn", "planet", "ringed giant"),
    ("Uranus", "planet", "rotates on its side"),
    ("Neptune", "planet", "outermost giant"),
    ("Apollo 11", "NASA", "first crewed Moon landing"),
    ("Voyager 1", "NASA", "interstellar probe"),
    ("Hubble Space Telescope", "NASA", "space observatory"),
    ("International Space Station", "NASA and partners", "orbital laboratory"),
    ("Chandrayaan-3", "ISRO", "Moon landing mission"),
    ("Artemis I", "NASA", "uncrewed lunar mission"),
    ("James Webb Space Telescope", "NASA", "infrared observatory"),
    ("Sputnik 1", "Soviet Union", "first artificial satellite"),
    ("Vostok 1", "Soviet Union", "first human spaceflight"),
    ("Shenzhou 5", "CNSA", "first Chinese crewed mission"),
    ("Rosetta", "ESA", "comet rendezvous mission"),
    ("Cassini-Huygens", "NASA and ESA", "Saturn mission"),
    ("Perseverance", "NASA", "Mars rover"),
)

SPACE_RACE = build_entries(*SPACE_ROWS)

ORGAN_ROWS = (
    ("Heart", "circulatory system", "pumps blood"),
    ("Lungs", "respiratory system", "exchange oxygen"),
    ("Brain", "nervous system", "controls the body"),
    ("Liver", "digestive system", "filters toxins"),
    ("Kidneys", "urinary system", "filter waste"),
    ("Stomach", "digestive system", "breaks down food"),
    ("Small intestine", "digestive system", "absorbs nutrients"),
    ("Large intestine", "digestive system", "absorbs water"),
    ("Pancreas", "digestive system", "produces insulin"),
    ("Spleen", "lymphatic system", "filters blood"),
    ("Skin", "integumentary system", "protects the body"),
    ("Bladder", "urinary system", "stores urine"),
    ("Thyroid", "endocrine system", "regulates metabolism"),
    ("Pituitary gland", "endocrine system", "controls other glands"),
    ("Esophagus", "digestive system", "moves food to the stomach"),
    ("Gallbladder", "digestive system", "stores bile"),
    ("Trachea", "respiratory system", "carries air"),
    ("Spinal cord", "nervous system", "relays nerve signals"),
    ("Bone marrow", "skeletal system", "makes blood cells"),
    ("Appendix", "digestive system", "small pouch off the large intestine"),
    ("Diaphragm", "respiratory system", "drives breathing"),
)

HUMAN_BODY = build_entries(*ORGAN_ROWS)

LAB_ROWS = (
    ("Microscope", "magnifies tiny objects", "biology"),
    ("Telescope", "observes distant objects", "astronomy"),
    ("Bunsen burner", "heats substances", "chemistry"),
    ("Beaker", "holds liquids", "chemistry"),
    ("Graduated cylinder", "measures liquid volume", "chemistry"),
    ("Pipette", "transfers small liquid volumes", "chemistry"),
    ("Thermometer", "measures temperature", "physics"),
    ("Centrifuge", "separates mixtures by spinning", "biology"),
    ("Petri dish", "cultures microorganisms", "biology"),
    ("Test tube", "holds small samples", "chemistry"),
    ("Voltmeter", "measures electric potential", "physics"),
    ("Ammeter", "measures electric current", "physics"),
    ("Spectrometer", "analyzes spectra", "physics"),
    ("Burette", "delivers measured liquid in titration", "chemistry"),
    ("Forceps", "grips small items", "biology"),
    ("Mortar and pestle", "grinds solids", "chemistry"),
    ("Hot plate", "heats containers", "chemistry"),
    ("pH meter", "measures acidity", "chemistry"),
    ("Balance", "measures mass", "physics"),
    ("Incubator", "maintains growth conditions", "biology"),
    ("Oscilloscope", "displays signal waveforms", "physics"),
)

LAB_ESSENTIALS = build_entries(*LAB_ROWS)

PHYSICS_ROWS = (
    ("Newton's first law", "Isaac Newton", "motion"),
    ("Newton's second law", "Isaac Newton", "force"),
    ("Newton's third law", "Isaac Newton", "action and reaction"),
    ("Law of universal gravitation", "Isaac Newton", "gravity"),
    ("Theory of relativity", "Albert Einstein", "space-time"),
    ("Photoelectric effect", "Albert Einstein", "light"),
    ("Principle of buoyancy", "Archimedes", "fluid mechanics"),
    ("Pascal's law", "Blaise Pascal", "pressure"),
    ("Ohm's law", "Georg Ohm", "electricity"),
    ("Faraday's law", "Michael Faraday", "electromagnetic induction"),
    ("Coulomb's law", "Charles-Augustin de Coulomb", "electrostatics"),
    ("Kepler's first law", "Johannes Kepler", "planetary orbits"),
    ("Hubble's law", "Edwin Hubble", "cosmology"),
    ("Bernoulli's principle", "Daniel Bernoulli", "fluid flow"),
    ("Hooke's law", "Robert Hooke", "elasticity"),
    ("Doppler effect", "Christian Doppler", "waves"),
    ("Pauli exclusion principle", "Wolfgang Pauli", "quantum physics"),
    ("Heisenberg uncertainty principle", "Werner Heisenberg", "quantum physics"),
    ("Schrodinger equation", "Erwin Schrodinger", "quantum physics"),
    ("Planck constant", "Max Planck", "quantum physics"),
    ("Foucault pendulum", "Leon Foucault", "Earth's rotation"),
)

PHYSICS_CHALLENGE = build_entries(*PHYSICS_ROWS)

EMPIRE_ROWS = (
    ("Roman Empire", "Mediterranean Europe", "Rome"),
    ("Ottoman Empire", "Anatolia and Southeast Europe", "Istanbul"),
    ("Mongol Empire", "Central Asia", "Karakorum"),
    ("Byzantine Empire", "Eastern Mediterranean", "Constantinople"),
    ("Persian Empire", "Iran", "Persepolis"),
    ("Maurya Empire", "Indian subcontinent", "Pataliputra"),
    ("Gupta Empire", "northern India", "Pataliputra"),
    ("Mughal Empire", "Indian subcontinent", "Agra"),
    ("Qing dynasty", "China", "Beijing"),
    ("Han dynasty", "China", "Chang'an"),
    ("Achaemenid Empire", "Iran", "Persepolis"),
    ("Aztec Empire", "central Mexico", "Tenochtitlan"),
    ("Inca Empire", "Andes", "Cusco"),
    ("Mali Empire", "West Africa", "Niani"),
    ("Songhai Empire", "West Africa", "Gao"),
    ("Assyrian Empire", "Mesopotamia", "Nineveh"),
    ("Babylonian Empire", "Mesopotamia", "Babylon"),
    ("Holy Roman Empire", "Central Europe", "Aachen"),
    ("Spanish Empire", "Iberia and overseas colonies", "Madrid"),
    ("British Empire", "global colonial empire", "London"),
    ("Russian Empire", "Eastern Europe and Asia", "Saint Petersburg"),
)

ANCIENT_EMPIRES = build_entries(*EMPIRE_ROWS)

WAR_ROWS = (
    ("World War I", "1914", "Allied Powers and Central Powers"),
    ("World War II", "1939", "Allies and Axis Powers"),
    ("American Civil War", "1861", "Union and Confederacy"),
    ("French Revolutionary Wars", "1792", "France and European monarchies"),
    ("Napoleonic Wars", "1803", "France and coalitions"),
    ("Korean War", "1950", "North Korea and South Korea"),
    ("Vietnam War", "1955", "North Vietnam and South Vietnam"),
    ("Gulf War", "1990", "Iraq and coalition forces"),
    ("Crimean War", "1853", "Russia and Ottoman-led alliance"),
    ("Thirty Years' War", "1618", "Catholics and Protestants"),
    ("Hundred Years' War", "1337", "England and France"),
    ("Peloponnesian War", "431 BCE", "Athens and Sparta"),
    ("War of the Spanish Succession", "1701", "Bourbons and Grand Alliance"),
    ("Seven Years' War", "1756", "Britain and France"),
    ("Russo-Japanese War", "1904", "Russia and Japan"),
    ("Spanish-American War", "1898", "Spain and United States"),
    ("Boer War", "1899", "British Empire and Boer republics"),
    ("Iran-Iraq War", "1980", "Iran and Iraq"),
    ("Winter War", "1939", "Soviet Union and Finland"),
    ("War of 1812", "1812", "United States and Britain"),
    ("Franco-Prussian War", "1870", "France and Prussia"),
)

WAR_TIMELINES = build_entries(*WAR_ROWS)

LEADER_ROWS = (
    ("Julius Caesar", "Rome", "statesman"),
    ("Cleopatra", "Egypt", "queen"),
    ("Alexander the Great", "Macedon", "king"),
    ("Napoleon Bonaparte", "France", "emperor"),
    ("Abraham Lincoln", "United States", "president"),
    ("Winston Churchill", "United Kingdom", "prime minister"),
    ("Mahatma Gandhi", "India", "independence leader"),
    ("Nelson Mandela", "South Africa", "president"),
    ("Margaret Thatcher", "United Kingdom", "prime minister"),
    ("George Washington", "United States", "president"),
    ("Akbar", "Mughal Empire", "emperor"),
    ("Elizabeth I", "England", "queen"),
    ("Simon Bolivar", "Venezuela", "liberator"),
    ("Otto von Bismarck", "Germany", "chancellor"),
    ("Genghis Khan", "Mongol Empire", "founder"),
    ("Sun Yat-sen", "China", "revolutionary"),
    ("Indira Gandhi", "India", "prime minister"),
    ("Franklin D. Roosevelt", "United States", "president"),
    ("Golda Meir", "Israel", "prime minister"),
    ("Kemal Ataturk", "Turkey", "president"),
    ("Angela Merkel", "Germany", "chancellor"),
)

FAMOUS_LEADERS = build_entries(*LEADER_ROWS)

REVOLUTION_ROWS = (
    ("French Revolution", "France", "1789"),
    ("American Revolution", "United States", "1775"),
    ("Russian Revolution", "Russia", "1917"),
    ("Industrial Revolution", "United Kingdom", "1760"),
    ("Iranian Revolution", "Iran", "1979"),
    ("Cuban Revolution", "Cuba", "1953"),
    ("Mexican Revolution", "Mexico", "1910"),
    ("Xinhai Revolution", "China", "1911"),
    ("Haitian Revolution", "Haiti", "1791"),
    ("Glorious Revolution", "England", "1688"),
    ("Egyptian Revolution", "Egypt", "2011"),
    ("People Power Revolution", "Philippines", "1986"),
    ("Velvet Revolution", "Czechoslovakia", "1989"),
    ("Portuguese Carnation Revolution", "Portugal", "1974"),
    ("Young Turk Revolution", "Ottoman Empire", "1908"),
    ("German Revolution", "Germany", "1918"),
    ("February Revolution", "Russia", "1917"),
    ("July Revolution", "France", "1830"),
    ("Hungarian Revolution", "Hungary", "1956"),
    ("Romanian Revolution", "Romania", "1989"),
    ("Tunisian Revolution", "Tunisia", "2010"),
)

MODERN_REVOLUTIONS = build_entries(*REVOLUTION_ROWS)

ARTWORK_ROWS = (
    ("Mona Lisa", "Leonardo da Vinci", "1503"),
    ("The Starry Night", "Vincent van Gogh", "1889"),
    ("The Last Supper", "Leonardo da Vinci", "1498"),
    ("Girl with a Pearl Earring", "Johannes Vermeer", "1665"),
    ("The Persistence of Memory", "Salvador Dali", "1931"),
    ("Guernica", "Pablo Picasso", "1937"),
    ("The Scream", "Edvard Munch", "1893"),
    ("The Night Watch", "Rembrandt", "1642"),
    ("American Gothic", "Grant Wood", "1930"),
    ("Las Meninas", "Diego Velazquez", "1656"),
    ("Water Lilies", "Claude Monet", "1906"),
    ("The Birth of Venus", "Sandro Botticelli", "1486"),
    ("The Kiss", "Gustav Klimt", "1908"),
    ("Whistler's Mother", "James McNeill Whistler", "1871"),
    ("Liberty Leading the People", "Eugene Delacroix", "1830"),
    ("Nighthawks", "Edward Hopper", "1942"),
    ("The Hay Wain", "John Constable", "1821"),
    ("Campbell's Soup Cans", "Andy Warhol", "1962"),
    ("A Sunday Afternoon on the Island of La Grande Jatte", "Georges Seurat", "1886"),
    ("The Son of Man", "Rene Magritte", "1964"),
    ("Arrangement in Grey and Black No. 1", "James McNeill Whistler", "1871"),
)

MASTERPIECES = build_entries(*ARTWORK_ROWS)

VISUAL_ARTIST_ROWS = (
    ("Leonardo da Vinci", "High Renaissance", "Italian"),
    ("Michelangelo", "High Renaissance", "Italian"),
    ("Raphael", "High Renaissance", "Italian"),
    ("Claude Monet", "Impressionism", "French"),
    ("Edgar Degas", "Impressionism", "French"),
    ("Vincent van Gogh", "Post-Impressionism", "Dutch"),
    ("Paul Cezanne", "Post-Impressionism", "French"),
    ("Pablo Picasso", "Cubism", "Spanish"),
    ("Georges Braque", "Cubism", "French"),
    ("Salvador Dali", "Surrealism", "Spanish"),
    ("Rene Magritte", "Surrealism", "Belgian"),
    ("Jackson Pollock", "Abstract Expressionism", "American"),
    ("Mark Rothko", "Abstract Expressionism", "American"),
    ("Andy Warhol", "Pop Art", "American"),
    ("Roy Lichtenstein", "Pop Art", "American"),
    ("Frida Kahlo", "Surrealism", "Mexican"),
    ("Diego Rivera", "Muralism", "Mexican"),
    ("Johannes Vermeer", "Dutch Golden Age", "Dutch"),
    ("Rembrandt", "Dutch Golden Age", "Dutch"),
    ("Gustav Klimt", "Symbolism", "Austrian"),
    ("Wassily Kandinsky", "Abstract art", "Russian"),
)

ART_MOVEMENTS = build_entries(*VISUAL_ARTIST_ROWS)

DESIGN_ROWS = (
    ("Eames Lounge Chair", "Charles and Ray Eames", "1956"),
    ("Barcelona Chair", "Ludwig Mies van der Rohe", "1929"),
    ("iPod", "Jonathan Ive", "2001"),
    ("Sydney Opera House", "Jorn Utzon", "1973"),
    ("Mini Cooper", "Alec Issigonis", "1959"),
    ("Lego minifigure", "Jens Nygaard Knudsen", "1978"),
    ("Coca-Cola contour bottle", "Earl R. Dean", "1915"),
    ("Bell 47 helicopter", "Arthur M. Young", "1945"),
    ("Vespa scooter", "Corradino D'Ascanio", "1946"),
    ("Chupa Chups logo", "Salvador Dali", "1969"),
    ("Swiss Army Knife", "Karl Elsener", "1897"),
    ("London Underground map", "Harry Beck", "1933"),
    ("Sony Walkman", "Norio Ohga", "1979"),
    ("Aeron chair", "Don Chadwick", "1994"),
    ("Typeface Helvetica", "Max Miedinger", "1957"),
    ("Burj Al Arab", "Tom Wright", "1999"),
    ("Google logo redesign", "Ruth Kedar", "1999"),
    ("Ferrari 250 GTO", "Sergio Scaglietti", "1962"),
    ("Dyson vacuum", "James Dyson", "1993"),
    ("Moleskine notebook revival", "Maria Sebregondi", "1997"),
    ("Alessi Juicy Salif", "Philippe Starck", "1990"),
)

DESIGN_ICONS = build_entries(*DESIGN_ROWS)

COLOR_ROWS = (
    ("Primary colors", "red, blue and yellow", "painting basics"),
    ("Secondary colors", "green, orange and purple", "mixed from primaries"),
    ("Complementary colors", "opposite colors on the wheel", "strong contrast"),
    ("Analogous colors", "neighboring hues", "harmonious palette"),
    ("Warm colors", "reds, oranges and yellows", "energetic mood"),
    ("Cool colors", "blues, greens and violets", "calm mood"),
    ("Tint", "hue mixed with white", "lighter variation"),
    ("Shade", "hue mixed with black", "darker variation"),
    ("Tone", "hue mixed with gray", "muted variation"),
    ("Saturation", "color intensity", "chroma control"),
    ("Hue", "basic color family", "wheel position"),
    ("Value", "lightness or darkness", "brightness scale"),
    ("RGB", "red, green and blue", "screen model"),
    ("CMYK", "cyan, magenta, yellow and key black", "print model"),
    ("Monochromatic palette", "single hue variations", "cohesive palette"),
    ("Triadic palette", "three evenly spaced hues", "balanced contrast"),
    ("Split-complementary palette", "one hue plus two near opposites", "balanced tension"),
    ("Color temperature", "warm versus cool bias", "visual mood"),
    ("Opacity", "transparency level", "alpha control"),
    ("Gradient", "smooth color transition", "blended range"),
    ("Contrast", "difference between colors", "readability aid"),
)

COLOR_THEORY = build_entries(*COLOR_ROWS)

VIRAL_ROWS = (
    ("Gangnam Style", "YouTube", "2012"),
    ("Harlem Shake", "YouTube", "2013"),
    ("Ice Bucket Challenge", "social media", "2014"),
    ("Doge", "internet forums", "2013"),
    ("Distracted Boyfriend", "stock photo meme pages", "2017"),
    ("Woman Yelling at a Cat", "Twitter", "2019"),
    ("Chewbacca Mom", "Facebook Live", "2016"),
    ("Salt Bae", "Instagram", "2017"),
    ("Grumpy Cat", "Reddit", "2012"),
    ("Hide the Pain Harold", "stock photo memes", "2011"),
    ("Coffin Dance", "TikTok", "2020"),
    ("Wednesday dance trend", "TikTok", "2022"),
    ("Bottle cap challenge", "Instagram", "2019"),
    ("Planking", "Facebook", "2011"),
    ("What color is the dress?", "Tumblr", "2015"),
    ("Damn Daniel", "Snapchat", "2016"),
    ("Charlie Bit My Finger", "YouTube", "2007"),
    ("Nyan Cat", "YouTube", "2011"),
    ("Leave Britney Alone", "YouTube", "2007"),
    ("Rickrolling", "YouTube", "2008"),
    ("Bernie Sanders mittens meme", "Twitter", "2021"),
)

VIRAL_MOMENTS = build_entries(*VIRAL_ROWS)

FANDOM_ROWS = (
    ("Marvel Cinematic Universe", "film franchise", "2008"),
    ("Star Wars", "multimedia franchise", "1977"),
    ("Harry Potter", "fantasy franchise", "1997"),
    ("Pokemon", "game and media franchise", "1996"),
    ("The Lord of the Rings", "fantasy franchise", "1954"),
    ("Doctor Who", "science fiction franchise", "1963"),
    ("Naruto", "manga and anime franchise", "1999"),
    ("One Piece", "manga and anime franchise", "1997"),
    ("Star Trek", "science fiction franchise", "1966"),
    ("DC Universe", "superhero franchise", "1938"),
    ("The Legend of Zelda", "game franchise", "1986"),
    ("Final Fantasy", "role-playing game franchise", "1987"),
    ("James Bond", "spy franchise", "1953"),
    ("Fast & Furious", "action film franchise", "2001"),
    ("Jurassic Park", "science fiction franchise", "1990"),
    ("Avatar", "science fiction film franchise", "2009"),
    ("Dune", "science fiction franchise", "1965"),
    ("The Hunger Games", "dystopian franchise", "2008"),
    ("Percy Jackson", "fantasy franchise", "2005"),
    ("Stranger Things", "television franchise", "2016"),
    ("The Witcher", "fantasy franchise", "1986"),
)

FANDOMS = build_entries(*FANDOM_ROWS)

def difficulty(item_index: int, template_index: int) -> float:
    template_bases = [0.24, 0.28, 0.36, 0.39, 0.46, 0.50, 0.56, 0.60, 0.68, 0.72]
    raw = template_bases[template_index % len(template_bases)] + ((item_index % 5) * 0.02)
    return round(min(raw, 0.88), 2)


def pick_distinct(items: list[str], banned: set[str], count: int) -> list[str]:
    selected: list[str] = []
    for item in items:
        if item in banned or item in selected:
            continue
        selected.append(item)
        if len(selected) == count:
            return selected
    raise ValueError(f"Unable to pick {count} distinct items from pool")


def make_options(correct: str, distractors: Iterable[str], seed: int) -> tuple[list[str], int]:
    options = [correct, *list(distractors)]
    rng = random.Random(seed)
    rng.shuffle(options)
    return options, options.index(correct)


def take_unique_strings(correct: str, candidates: Iterable[str], count: int = 3) -> list[str]:
    selected: list[str] = []
    seen = {correct}
    for candidate in candidates:
        if candidate in seen:
            continue
        selected.append(candidate)
        seen.add(candidate)
        if len(selected) == count:
            return selected
    raise ValueError(f"Unable to collect {count} unique distractors for {correct}")


def statement(entry: Entry, prop_label: str, value: str) -> str:
    return f"{entry.name} -> {prop_label}: {value}"


def pair_card(spec: CategorySpec, value_a: str, value_b: str) -> str:
    return f"{spec.prop_a_label}: {value_a} | {spec.prop_b_label}: {value_b}"


def render_question(text: str, options: list[str], answer: int, question_difficulty: float) -> str:
    escaped_question = text.replace("\\", "\\\\").replace('"', '\\"')
    escaped_options = [option.replace("\\", "\\\\").replace('"', '\\"') for option in options]
    options_block = ", ".join(f"\"{option}\"" for option in escaped_options)
    return f'        Question("{escaped_question}", listOf({options_block}), {answer}, difficulty = {question_difficulty:.2f}),'


def choose_variant(index: int, shift: int, variants: list[str]) -> str:
    return variants[(index + shift) % len(variants)]


def stable_seed(*parts: str) -> int:
    total = 0
    for part in parts:
        for ch in part:
            total = (total * 131 + ord(ch)) % (2**31 - 1)
    return total


def numeric_value(value: str) -> int | None:
    text = value.replace(",", "").strip()
    if re.fullmatch(r"-?\d+", text):
        return int(text)
    if re.fullmatch(r"\d+\s*BCE", text, flags=re.I):
        return -int(re.sub(r"\D", "", text))
    return None


def text_similarity(a: str, b: str) -> float:
    from difflib import SequenceMatcher

    return SequenceMatcher(None, a.lower(), b.lower()).ratio()


def pick_value_distractors(correct: str, candidates: list[str], count: int, seed: int) -> list[str]:
    correct_num = numeric_value(correct)
    ranked: list[tuple[float, int, str]] = []
    for candidate in candidates:
        candidate_num = numeric_value(candidate)
        if correct_num is not None and candidate_num is not None:
            score = -abs(correct_num - candidate_num)
        else:
            score = text_similarity(correct, candidate)
        ranked.append((score, stable_seed(correct, candidate, str(seed)), candidate))
    ranked.sort(key=lambda item: (-item[0], item[1]))
    ordered = [candidate for _, _, candidate in ranked]
    return pick_distinct(ordered, {correct}, count)


def pick_name_distractors(entry: Entry, entries: list[Entry], count: int, seed: int) -> list[str]:
    ranked: list[tuple[float, int, str]] = []
    for candidate in entries:
        if candidate.name == entry.name:
            continue
        score = 0.0
        if candidate.prop_a == entry.prop_a:
            score += 4.0
        if candidate.prop_b == entry.prop_b:
            score += 4.0
        score += text_similarity(candidate.name, entry.name)
        score += 0.35 * text_similarity(candidate.prop_a, entry.prop_a)
        score += 0.35 * text_similarity(candidate.prop_b, entry.prop_b)
        ranked.append((score, stable_seed(entry.name, candidate.name, str(seed)), candidate.name))
    ranked.sort(key=lambda item: (-item[0], item[1]))
    ordered = [name for _, _, name in ranked]
    return pick_distinct(ordered, {entry.name}, count)


def rebalance_answer_positions(
    questions: list[tuple[str, list[str], int, float]],
    category_key: str,
) -> list[tuple[str, list[str], int, float]]:
    base = len(questions) // 4
    remainder = len(questions) % 4
    extra_positions = list(range(4))
    rng = random.Random(stable_seed(category_key, "answer-balance"))
    rng.shuffle(extra_positions)
    counts = {index: base for index in range(4)}
    for index in extra_positions[:remainder]:
        counts[index] += 1

    targets: list[int] = []
    for index in range(4):
        targets.extend([index] * counts[index])
    rng.shuffle(targets)

    balanced: list[tuple[str, list[str], int, float]] = []
    for question, target in zip(questions, targets):
        text, options, answer_index, difficulty_value = question
        shift = (target - answer_index) % len(options)
        rotated = options[-shift:] + options[:-shift] if shift else list(options)
        balanced.append((text, rotated, target, difficulty_value))
    return balanced


def generate_questions(spec: CategorySpec) -> list[tuple[str, list[str], int, float]]:
    entries = list(spec.entries)
    names = [entry.name for entry in entries]
    prop_a_values = [entry.prop_a for entry in entries]
    prop_b_values = [entry.prop_b for entry in entries]
    questions: list[tuple[str, list[str], int, float]] = []
    seen: set[str] = set()

    for index, entry in enumerate(entries):
        other_names = [name for name in names if name != entry.name]
        other_a_values = [value for value in prop_a_values if value != entry.prop_a]
        other_b_values = [value for value in prop_b_values if value != entry.prop_b]

        def pick_entry(predicate, skip: set[str] | None = None) -> Entry:
            skip = skip or set()
            for step in range(1, len(entries)):
                candidate = entries[(index + step) % len(entries)]
                if candidate.name == entry.name or candidate.name in skip:
                    continue
                if predicate(candidate):
                    return candidate
            raise ValueError(f"Unable to find fallback entry for {spec.genre} / {spec.category} / {entry.name}")

        wrong_a_entry = pick_entry(lambda candidate: candidate.prop_a != entry.prop_a)
        wrong_b_entry = pick_entry(lambda candidate: candidate.prop_b != entry.prop_b)
        alt_a_1 = pick_entry(lambda candidate: candidate.prop_a != entry.prop_a, {wrong_a_entry.name})
        alt_a_2 = pick_entry(lambda candidate: candidate.prop_a != entry.prop_a, {wrong_a_entry.name, alt_a_1.name})
        alt_b_1 = pick_entry(lambda candidate: candidate.prop_b != entry.prop_b, {wrong_b_entry.name})
        alt_b_2 = pick_entry(lambda candidate: candidate.prop_b != entry.prop_b, {wrong_b_entry.name, alt_b_1.name})
        pair_matches = [candidate for candidate in entries if candidate.prop_a == entry.prop_a and candidate.prop_b == entry.prop_b]
        pair_is_unique = len(pair_matches) == 1

        templates: list[tuple[str, list[str], int]] = []
        prefix = f"{spec.category}: "
        variant_seed = sum(ord(ch) for ch in entry.name)

        def pick_prompt(shift: int, variants: list[str]) -> str:
            ordered = [choose_variant(variant_seed, shift + offset, variants) for offset in range(len(variants))]
            for candidate in ordered:
                full_text = prefix + candidate
                if full_text not in seen:
                    return full_text
            return prefix + ordered[0] + f" [{entry.name}]"

        opts, answer = make_options(
            entry.prop_a,
            pick_value_distractors(entry.prop_a, other_a_values, 3, seed=index * 100 + 1),
            seed=index * 100 + 1,
        )
        templates.append(
            (
                pick_prompt(
                    0,
                    [
                        f"What is the {spec.prop_a_label} of {entry.name}?",
                        f"Which {spec.prop_a_label} belongs to {entry.name}?",
                        f"Choose the correct {spec.prop_a_label} for {entry.name}.",
                        f"{entry.name} is linked to which {spec.prop_a_label}?",
                    ],
                ),
                opts,
                answer,
            )
        )

        opts, answer = make_options(
            entry.prop_b,
            pick_value_distractors(entry.prop_b, other_b_values, 3, seed=index * 100 + 2),
            seed=index * 100 + 2,
        )
        templates.append(
            (
                pick_prompt(
                    1,
                    [
                        f"What is the {spec.prop_b_label} of {entry.name}?",
                        f"Which {spec.prop_b_label} belongs to {entry.name}?",
                        f"Choose the correct {spec.prop_b_label} for {entry.name}.",
                        f"{entry.name} is linked to which {spec.prop_b_label}?",
                    ],
                ),
                opts,
                answer,
            )
        )

        if pair_is_unique:
            opts, answer = make_options(
                entry.name,
                pick_name_distractors(entry, entries, 3, seed=index * 100 + 3),
                seed=index * 100 + 3,
            )
            templates.append(
                (
                    pick_prompt(
                        2,
                        [
                            f"If the {spec.prop_a_label} is {entry.prop_a} and the {spec.prop_b_label} is {entry.prop_b}, which {spec.entity_label} fits?",
                            f"Which {spec.entity_label} matches the clue pair {spec.prop_a_label} {entry.prop_a} and {spec.prop_b_label} {entry.prop_b}?",
                            f"One clue says {spec.prop_a_label} {entry.prop_a}; another says {spec.prop_b_label} {entry.prop_b}. What is the {spec.entity_label}?",
                            f"Use both clues to name the {spec.entity_label}: {spec.prop_a_label} {entry.prop_a} and {spec.prop_b_label} {entry.prop_b}.",
                        ],
                    ),
                    opts,
                    answer,
                )
            )

            opts, answer = make_options(
                entry.name,
                pick_name_distractors(entry, entries, 3, seed=index * 100 + 4),
                seed=index * 100 + 4,
            )
            templates.append(
                (
                    pick_prompt(
                        3,
                        [
                            f"{entry.prop_b} belongs to which {spec.entity_label} when the {spec.prop_a_label} clue is {entry.prop_a}?",
                            f"Which {spec.entity_label} goes with {spec.prop_b_label} {entry.prop_b} and {spec.prop_a_label} {entry.prop_a}?",
                            f"Pair the clues {spec.prop_b_label} {entry.prop_b} and {spec.prop_a_label} {entry.prop_a}. Which {spec.entity_label} do they describe?",
                            f"If {spec.prop_b_label} is {entry.prop_b} and {spec.prop_a_label} is {entry.prop_a}, which {spec.entity_label} matches that clue set?",
                        ],
                    ),
                    opts,
                    answer,
                )
            )
        else:
            correct_card = pair_card(spec, entry.prop_a, entry.prop_b)
            distractor_cards = take_unique_strings(
                correct_card,
                [
                    pair_card(spec, entry.prop_a, wrong_b_entry.prop_b),
                    pair_card(spec, wrong_a_entry.prop_a, entry.prop_b),
                    pair_card(spec, alt_a_1.prop_a, alt_b_1.prop_b),
                    pair_card(spec, alt_a_2.prop_a, alt_b_2.prop_b),
                    pair_card(spec, wrong_a_entry.prop_a, wrong_b_entry.prop_b),
                ],
            )
            opts, answer = make_options(correct_card, distractor_cards, seed=index * 100 + 3)
            templates.append(
                (
                    pick_prompt(
                        2,
                        [
                            f"Which clue card belongs to {entry.name}?",
                            f"Pick the correct two-part profile for {entry.name}.",
                            f"Which fact card best matches {entry.name}?",
                            f"Choose the clue pair that fits {entry.name}.",
                        ],
                    ),
                    opts,
                    answer,
                )
            )

            opts, answer = make_options(
                correct_card,
                take_unique_strings(
                    correct_card,
                    [
                        pair_card(spec, entry.prop_a, wrong_b_entry.prop_b),
                        pair_card(spec, wrong_a_entry.prop_a, entry.prop_b),
                        pair_card(spec, alt_a_1.prop_a, alt_b_1.prop_b),
                        pair_card(spec, alt_a_2.prop_a, alt_b_2.prop_b),
                        pair_card(spec, wrong_a_entry.prop_a, wrong_b_entry.prop_b),
                    ],
                ),
                seed=index * 100 + 4,
            )
            templates.append(
                (
                    pick_prompt(
                        3,
                        [
                            f"If you were labeling {entry.name}, which clue pair would be accurate?",
                            f"Which profile line-up is correct for {entry.name}?",
                            f"Choose the right fact pair for {entry.name}.",
                            f"Which two clues can you safely attach to {entry.name}?",
                        ],
                    ),
                    opts,
                    answer,
                )
            )

        def false_statement_for(candidate: Entry, attr_key: str, seed: int) -> str:
            if attr_key == "a":
                false_value = pick_value_distractors(candidate.prop_a, [value for value in prop_a_values if value != candidate.prop_a], 1, seed)[0]
                return statement(candidate, spec.prop_a_label, false_value)
            false_value = pick_value_distractors(candidate.prop_b, [value for value in prop_b_values if value != candidate.prop_b], 1, seed)[0]
            return statement(candidate, spec.prop_b_label, false_value)

        correct = statement(entry, spec.prop_a_label, entry.prop_a)
        distractors = take_unique_strings(correct, [
            false_statement_for(alt_a_1, "a", index * 100 + 51),
            false_statement_for(alt_a_2, "a", index * 100 + 52),
            false_statement_for(entries[(index + 8) % len(entries)], "a", index * 100 + 53),
            false_statement_for(entries[(index + 15) % len(entries)], "a", index * 100 + 54),
            false_statement_for(entries[(index + 17) % len(entries)], "a", index * 100 + 55),
        ])
        opts, answer = make_options(correct, distractors, seed=index * 100 + 5)
        templates.append(
            (
                pick_prompt(
                    4,
                    [
                        f"Which option correctly matches a {spec.entity_label} to its {spec.prop_a_label} in the {entry.name} round?",
                        f"Pick the accurate {spec.entity_label}-to-{spec.prop_a_label} pairing in the {entry.name} round.",
                        f"Which pairing keeps the {spec.entity_label} and {spec.prop_a_label} together correctly for the {entry.name} clue?",
                        f"Select the only valid {spec.prop_a_label} match in the {entry.name} set.",
                    ],
                ),
                opts,
                answer,
            )
        )

        correct = statement(entry, spec.prop_b_label, entry.prop_b)
        distractors = take_unique_strings(correct, [
            false_statement_for(alt_b_1, "b", index * 100 + 61),
            false_statement_for(alt_b_2, "b", index * 100 + 62),
            false_statement_for(entries[(index + 5) % len(entries)], "b", index * 100 + 63),
            false_statement_for(entries[(index + 15) % len(entries)], "b", index * 100 + 64),
            false_statement_for(entries[(index + 17) % len(entries)], "b", index * 100 + 65),
        ])
        opts, answer = make_options(correct, distractors, seed=index * 100 + 6)
        templates.append(
            (
                pick_prompt(
                    5,
                    [
                        f"Which option correctly matches a {spec.entity_label} to its {spec.prop_b_label} in the {entry.name} round?",
                        f"Pick the accurate {spec.entity_label}-to-{spec.prop_b_label} pairing in the {entry.name} round.",
                        f"Which pairing keeps the {spec.entity_label} and {spec.prop_b_label} together correctly for the {entry.name} clue?",
                        f"Select the only valid {spec.prop_b_label} match in the {entry.name} set.",
                    ],
                ),
                opts,
                answer,
            )
        )

        correct = false_statement_for(entry, "a", index * 100 + 71)
        distractors = take_unique_strings(correct, [
            statement(entry, spec.prop_a_label, entry.prop_a),
            statement(alt_a_1, spec.prop_a_label, alt_a_1.prop_a),
            statement(alt_a_2, spec.prop_a_label, alt_a_2.prop_a),
            statement(entries[(index + 15) % len(entries)], spec.prop_a_label, entries[(index + 15) % len(entries)].prop_a),
            statement(entries[(index + 16) % len(entries)], spec.prop_a_label, entries[(index + 16) % len(entries)].prop_a),
        ])
        opts, answer = make_options(correct, distractors, seed=index * 100 + 7)
        templates.append(
            (
                pick_prompt(
                    6,
                    [
                        f"Find the mismatch in these {spec.entity_label}-to-{spec.prop_a_label} pairings for the {entry.name} clue.",
                        f"Which option breaks the {spec.prop_a_label} mapping in the {entry.name} round?",
                        f"One {spec.prop_a_label} pairing is wrong for the {entry.name} clue. Which one is it?",
                        f"Which pairing mislabels the {spec.prop_a_label} in the {entry.name} set?",
                    ],
                ),
                opts,
                answer,
            )
        )

        correct = false_statement_for(entry, "b", index * 100 + 81)
        distractors = take_unique_strings(correct, [
            statement(entry, spec.prop_b_label, entry.prop_b),
            statement(alt_b_1, spec.prop_b_label, alt_b_1.prop_b),
            statement(alt_b_2, spec.prop_b_label, alt_b_2.prop_b),
            statement(entries[(index + 15) % len(entries)], spec.prop_b_label, entries[(index + 15) % len(entries)].prop_b),
            statement(entries[(index + 16) % len(entries)], spec.prop_b_label, entries[(index + 16) % len(entries)].prop_b),
        ])
        opts, answer = make_options(correct, distractors, seed=index * 100 + 8)
        templates.append(
            (
                pick_prompt(
                    7,
                    [
                        f"Find the mismatch in these {spec.entity_label}-to-{spec.prop_b_label} pairings for the {entry.name} clue.",
                        f"Which option breaks the {spec.prop_b_label} mapping in the {entry.name} round?",
                        f"One {spec.prop_b_label} pairing is wrong for the {entry.name} clue. Which one is it?",
                        f"Which pairing mislabels the {spec.prop_b_label} in the {entry.name} set?",
                    ],
                ),
                opts,
                answer,
            )
        )

        if pair_is_unique:
            opts, answer = make_options(
                entry.name,
                pick_name_distractors(entry, entries, 3, seed=index * 100 + 9),
                seed=index * 100 + 9,
            )
            templates.append(
                (
                    pick_prompt(
                        8,
                        [
                            f"Which {spec.entity_label} fits both clues: {spec.prop_a_label} {entry.prop_a} and {spec.prop_b_label} {entry.prop_b}?",
                            f"Use both clues to identify the {spec.entity_label}: {spec.prop_a_label} {entry.prop_a} and {spec.prop_b_label} {entry.prop_b}.",
                            f"Which {spec.entity_label} matches this double clue: {spec.prop_a_label} {entry.prop_a} plus {spec.prop_b_label} {entry.prop_b}?",
                            f"Two facts are given, {spec.prop_a_label} {entry.prop_a} and {spec.prop_b_label} {entry.prop_b}. Which {spec.entity_label} do they point to?",
                        ],
                    ),
                    opts,
                    answer,
                )
            )
        else:
            correct_card = pair_card(spec, entry.prop_a, entry.prop_b)
            opts, answer = make_options(
                correct_card,
                take_unique_strings(
                    correct_card,
                    [
                        pair_card(spec, entry.prop_a, wrong_b_entry.prop_b),
                        pair_card(spec, wrong_a_entry.prop_a, entry.prop_b),
                        pair_card(spec, alt_a_1.prop_a, alt_b_2.prop_b),
                        pair_card(spec, alt_a_2.prop_a, alt_b_1.prop_b),
                        pair_card(spec, wrong_a_entry.prop_a, wrong_b_entry.prop_b),
                    ],
                ),
                seed=index * 100 + 9,
            )
            templates.append(
                (
                    pick_prompt(
                        8,
                        [
                            f"Which clue pair best captures {entry.name}?",
                            f"Pick the most accurate profile card for {entry.name}.",
                            f"Which combined clue set belongs to {entry.name}?",
                            f"Choose the fact card that cleanly identifies {entry.name}.",
                        ],
                    ),
                    opts,
                    answer,
                )
            )

        true_combined = f"{entry.name} pairs {spec.prop_a_label} {entry.prop_a} with {spec.prop_b_label} {entry.prop_b}."
        true_a = f"{entry.name} is linked to {spec.prop_a_label} {entry.prop_a}."
        true_b = f"{entry.name} is linked to {spec.prop_b_label} {entry.prop_b}."
        false_combined = f"{entry.name} pairs {spec.prop_a_label} {wrong_a_entry.prop_a} with {spec.prop_b_label} {wrong_b_entry.prop_b}."
        false_a = f"{entry.name} is linked to {spec.prop_a_label} {wrong_a_entry.prop_a}."
        false_b = f"{entry.name} is linked to {spec.prop_b_label} {wrong_b_entry.prop_b}."

        if index % 2 == 0:
            correct = true_combined
            distractors = take_unique_strings(correct, [false_combined, false_a, false_b, true_a, true_b])
            prompt = pick_prompt(
                9,
                [
                    f"Which statement about {entry.name} is accurate?",
                    f"Choose the true summary of {entry.name}.",
                    f"Which sentence correctly describes {entry.name}?",
                        f"Select the only accurate note about {entry.name}.",
                ],
            )
        else:
            correct = false_combined
            distractors = take_unique_strings(correct, [true_combined, true_a, true_b, false_a, false_b])
            prompt = pick_prompt(
                10,
                [
                    f"Which statement about {entry.name} is not correct?",
                    f"Choose the inaccurate summary of {entry.name}.",
                    f"Which sentence misdescribes {entry.name}?",
                    f"Select the false note about {entry.name}.",
                ],
            )
        opts, answer = make_options(correct, distractors, seed=index * 100 + 10)
        templates.append((prompt, opts, answer))

        for template_index, (text, options, answer_index) in enumerate(templates):
            if text in seen:
                raise ValueError(f"Duplicate question text generated: {text}")
            seen.add(text)
            questions.append((text, options, answer_index, difficulty(index, template_index)))

    if len(questions) < 210:
        raise ValueError(f"{spec.genre} / {spec.category} produced {len(questions)} questions instead of at least 210")
    return rebalance_answer_positions(questions[:210], f"{spec.genre}|{spec.category}")


def camel_case(text: str) -> str:
    parts = [part for part in re.split(r"[^A-Za-z0-9]+", text) if part]
    if not parts:
        return "generated"
    return parts[0].lower() + "".join(part[:1].upper() + part[1:] for part in parts[1:])


def category_variable_name(spec: CategorySpec) -> str:
    return f"{camel_case(spec.genre)}{camel_case(spec.category)[:1].upper()}{camel_case(spec.category)[1:]}Questions"


def genre_variable_name(genre: str) -> str:
    return f"{camel_case(genre)}Questions"


def category_map_variable_name(genre: str) -> str:
    return f"{camel_case(genre)}CategoryMap"


def upper_camel_case(text: str) -> str:
    parts = [part for part in re.split(r"[^A-Za-z0-9]+", text) if part]
    if not parts:
        return "Generated"
    return "".join(part[:1].upper() + part[1:] for part in parts)


def bank_object_name(spec: CategorySpec) -> str:
    return f"{upper_camel_case(spec.genre)}{upper_camel_case(spec.category)}QuestionBank"


def category_specs() -> tuple[CategorySpec, ...]:
    return (
        CategorySpec("Sports", "Cricket Legends", "player", "national team", "primary role", CRICKET_LEGENDS),
        CategorySpec("Sports", "FIFA World Cup", "tournament edition", "champion", "host nation", WORLD_CUPS),
        CategorySpec("Sports", "NBA All-Stars", "player", "position", "signature franchise", NBA_ALL_STARS),
        CategorySpec("Sports", "Tennis Grand Slams", "player", "country", "signature Slam", TENNIS_GRAND_SLAMS),
        CategorySpec("Movies", "Blockbuster Trivia", "film", "director", "release year", BLOCKBUSTERS),
        CategorySpec("Movies", "Award Winners", "Best Picture winner", "director", "Oscar ceremony year", AWARD_WINNERS),
        CategorySpec("Movies", "Cult Classics", "cult film", "director", "release year", CULT_CLASSICS),
        CategorySpec("Movies", "Franchise Showdown", "character", "franchise film", "release year", FRANCHISE_CHARACTERS),
        CategorySpec("Science", "Space Race", "space mission or object", "agency or class", "key fact", SPACE_RACE),
        CategorySpec("Science", "Human Body", "organ", "body system", "primary function", HUMAN_BODY),
        CategorySpec("Science", "Lab Essentials", "tool", "main use", "scientific field", LAB_ESSENTIALS),
        CategorySpec("Science", "Physics Challenge", "concept", "scientist", "topic", PHYSICS_CHALLENGE),
        CategorySpec("History", "Ancient Empires", "empire", "core region", "capital", ANCIENT_EMPIRES),
        CategorySpec("History", "War Timelines", "conflict", "starting year", "main sides", WAR_TIMELINES),
        CategorySpec("History", "Famous Leaders", "leader", "country or state", "title", FAMOUS_LEADERS),
        CategorySpec("History", "Modern Revolutions", "revolution", "country", "breakout year", MODERN_REVOLUTIONS),
        CategorySpec("Music", "Chart Toppers", "song", "artist", "release year", CHART_SONGS),
        CategorySpec("Music", "Classic Albums", "album", "artist", "release year", CLASSIC_ALBUMS),
        CategorySpec("Music", "Global Hits", "artist", "home country", "genre", GLOBAL_ARTISTS),
        CategorySpec("Music", "Name That Artist", "song", "artist", "album", NAME_THAT_ARTIST),
        CategorySpec("Tech", "Startup Stories", "company", "founded year", "origin country", STARTUPS),
        CategorySpec("Tech", "Coding Basics", "programming language", "creator", "core style", LANGUAGES),
        CategorySpec("Tech", "Big Tech", "company", "origin country", "founded year", BIG_TECH),
        CategorySpec("Tech", "Future Trends", "technology term", "expansion or plain form", "core use", FUTURE_TECH),
        CategorySpec("Entertainment", "TV Obsession", "series", "network or platform", "debut year", TV_SHOWS),
        CategorySpec("Entertainment", "Celebrity Buzz", "star", "screen role", "home country", CELEBRITIES),
        CategorySpec("Entertainment", "Streaming Picks", "series", "platform", "genre", STREAMING_SHOWS),
        CategorySpec("Entertainment", "Award Night", "award show", "field", "home city", AWARD_SHOWS),
        CategorySpec("Geography", "Capitals Sprint", "country", "capital", "continent", CAPITAL_COUNTRIES),
        CategorySpec("Geography", "World Maps", "country", "continent", "capital", CARTOGRAPHY_COUNTRIES),
        CategorySpec("Geography", "Landmark Hunt", "landmark", "country", "city or region", LANDMARKS),
        CategorySpec("Geography", "Country Flags", "country", "flag motif", "flag colors", FLAG_COUNTRIES),
        CategorySpec("Art", "Masterpieces", "artwork", "artist", "year completed", MASTERPIECES),
        CategorySpec("Art", "Art Movements", "artist", "movement", "nationality", ART_MOVEMENTS),
        CategorySpec("Art", "Design Icons", "design icon", "designer", "debut year", DESIGN_ICONS),
        CategorySpec("Art", "Color Theory", "color term", "meaning", "design use", COLOR_THEORY),
        CategorySpec("Literature", "Classic Authors", "book", "author", "publication year", CLASSIC_BOOKS),
        CategorySpec("Literature", "Book Worlds", "character", "book", "author", BOOK_WORLDS),
        CategorySpec("Literature", "Poetry Picks", "poem", "poet", "publication year", POEMS),
        CategorySpec("Literature", "Modern Fiction", "novel", "author", "publication year", MODERN_BOOKS),
        CategorySpec("General Knowledge", "General Knowledge Spotlight", "invention", "inventor", "breakthrough year", INVENTIONS),
        CategorySpec("General Knowledge", "General Knowledge Pro", "country", "currency", "capital", CURRENCY_COUNTRIES),
        CategorySpec("General Knowledge", "General Knowledge Deep Dive", "myth figure", "mythology", "domain", MYTH_FIGURES),
        CategorySpec("General Knowledge", "General Knowledge Finals", "element", "symbol", "atomic number", ELEMENTS),
        CategorySpec("Pop Culture", "Fandom Frenzy", "franchise", "medium", "debut year", FANDOMS),
        CategorySpec("Pop Culture", "Viral Moments", "viral moment", "main platform", "peak year", VIRAL_MOMENTS),
        CategorySpec("Pop Culture", "Series Lore", "character", "series", "platform", SERIES_CHARACTERS),
        CategorySpec("Pop Culture", "Gaming Culture", "video game", "developer", "release year", GAMES),
    )


def build_genre_index(specs: tuple[CategorySpec, ...]) -> tuple[list[str], dict[str, list[CategorySpec]], dict[CategorySpec, list[tuple[str, list[str], int, float]]]]:
    questions_by_spec = {spec: generate_questions(spec) for spec in specs}
    total = sum(len(questions) for questions in questions_by_spec.values())
    if total != 10_080:
        raise ValueError(f"Expected 10080 questions, generated {total}")

    ordered_genres = [
        "Sports",
        "Movies",
        "Science",
        "History",
        "Music",
        "Tech",
        "Entertainment",
        "Geography",
        "Art",
        "Literature",
        "General Knowledge",
        "Pop Culture",
    ]
    specs_by_genre: dict[str, list[CategorySpec]] = {genre: [] for genre in ordered_genres}
    for spec in specs:
        specs_by_genre[spec.genre].append(spec)

    return ordered_genres, specs_by_genre, questions_by_spec


def render_question_banks(
    ordered_genres: list[str],
    specs_by_genre: dict[str, list[CategorySpec]],
    questions_by_spec: dict[CategorySpec, list[tuple[str, list[str], int, float]]],
) -> str:
    lines = [
        "package com.triviaroyale.data",
        "",
        "// Generated question banks with 48 themed category pools and 10,080 total questions.",
    ]

    for genre in ordered_genres:
        for spec in specs_by_genre[genre]:
            lines.append("")
            lines.append(f"object {bank_object_name(spec)} {{")
            lines.append("    val questions = listOf(")
            for question_text, options, answer_index, question_difficulty in questions_by_spec[spec]:
                lines.append(render_question(question_text, options, answer_index, question_difficulty))
            lines.append("    )")
            lines.append("}")

    return "\n".join(lines) + "\n"


def render_repository(ordered_genres: list[str], specs_by_genre: dict[str, list[CategorySpec]]) -> str:
    lines = [
        "package com.triviaroyale.data",
        "",
        "data class Question(",
        "    val question: String,",
        "    val options: List<String>,",
        "    val answer: Int, // index of correct answer",
        "    val difficulty: Double = 0.5",
        ")",
        "",
        "object QuizRepository {",
        "    // Generated quiz repository that indexes per-category question banks.",
    ]

    for genre in ordered_genres:
        for spec in specs_by_genre[genre]:
            lines.append(f"    private val {category_variable_name(spec)} by lazy {{ {bank_object_name(spec)}.questions }}")
        lines.append("")
        join_expr = " + ".join(category_variable_name(spec) for spec in specs_by_genre[genre])
        lines.append(f"    private val {genre_variable_name(genre)} by lazy {{ {join_expr} }}")
        map_lines = ", ".join(f"\"{spec.category}\" to {category_variable_name(spec)}" for spec in specs_by_genre[genre])
        lines.append(f"    private val {category_map_variable_name(genre)} by lazy {{ mapOf({map_lines}) }}")
        lines.append("")

    lines.extend(
        [
            "    private val dailyChallengeQuestions by lazy {",
            "        (",
            "        sportsQuestions.take(8) +",
            "        moviesQuestions.take(8) +",
            "        scienceQuestions.take(8) +",
            "        historyQuestions.take(8) +",
            "        musicQuestions.take(8) +",
            "        techQuestions.take(8) +",
            "        entertainmentQuestions.take(8) +",
            "        geographyQuestions.take(8) +",
            "        artQuestions.take(8) +",
            "        literatureQuestions.take(8) +",
            "        generalKnowledgeQuestions.take(8) +",
            "        popCultureQuestions.take(8)",
            "        )",
            "    }",
            "",
            "    private val lightningQuestions by lazy {",
            "        (",
            "        sportsQuestions.take(24) +",
            "        moviesQuestions.take(24) +",
            "        scienceQuestions.take(24) +",
            "        historyQuestions.take(24) +",
            "        musicQuestions.take(24) +",
            "        techQuestions.take(24) +",
            "        entertainmentQuestions.take(24) +",
            "        geographyQuestions.take(24) +",
            "        artQuestions.take(24) +",
            "        literatureQuestions.take(24)",
            "        )",
            "    }",
            "",
            "    private val genreMap by lazy {",
            "        mapOf(",
            '            "Daily Challenge" to dailyChallengeQuestions,',
            '            "Sports" to sportsQuestions,',
            '            "Movies" to moviesQuestions,',
            '            "Science" to scienceQuestions,',
            '            "History" to historyQuestions,',
            '            "Music" to musicQuestions,',
            '            "Tech" to techQuestions,',
            '            "Entertainment" to entertainmentQuestions,',
            '            "Geography" to geographyQuestions,',
            '            "Art" to artQuestions,',
            '            "Literature" to literatureQuestions,',
            '            "General Knowledge" to generalKnowledgeQuestions,',
            '            "Pop Culture" to popCultureQuestions,',
            "        )",
            "    }",
            "",
            "    private val categoryMapByGenre by lazy {",
            "        mapOf(",
            '            "Sports" to sportsCategoryMap,',
            '            "Movies" to moviesCategoryMap,',
            '            "Science" to scienceCategoryMap,',
            '            "History" to historyCategoryMap,',
            '            "Music" to musicCategoryMap,',
            '            "Tech" to techCategoryMap,',
            '            "Entertainment" to entertainmentCategoryMap,',
            '            "Geography" to geographyCategoryMap,',
            '            "Art" to artCategoryMap,',
            '            "Literature" to literatureCategoryMap,',
            '            "General Knowledge" to generalKnowledgeCategoryMap,',
            '            "Pop Culture" to popCultureCategoryMap,',
            "        )",
            "    }",
            "",
            "    fun getQuestions(genre: String? = null, count: Int = 10): List<Question> {",
            "        val pool = if (genre != null) genreMap[genre] ?: generalKnowledgeQuestions else generalKnowledgeQuestions",
            "        return pool.shuffled().take(count.coerceAtMost(pool.size))",
            "    }",
            "",
            "    fun getQuestionsForCategory(genre: String, category: String, count: Int = 10): List<Question> {",
            "        val pool = categoryMapByGenre[genre]?.get(category) ?: genreMap[genre] ?: generalKnowledgeQuestions",
            "        return pool.shuffled().take(count.coerceAtMost(pool.size))",
            "    }",
            "",
            "    fun getLightningRoundQuestions(count: Int = 30): List<Question> {",
            "        return lightningQuestions.shuffled().take(count.coerceAtMost(lightningQuestions.size))",
            "    }",
            "",
            "    fun getDailyChallengeQuestions(count: Int = 8): List<Question> {",
            "        return dailyChallengeQuestions.shuffled().take(count.coerceAtMost(dailyChallengeQuestions.size))",
            "    }",
            "",
            "    data class Genre(",
            "        val name: String,",
            "        val icon: String, // Material icon name",
            "        val quizCount: Int",
            "    )",
            "",
            "    val genres by lazy {",
            "        listOf(",
            '            Genre("Sports", "sports_basketball", sportsQuestions.size),',
            '            Genre("Movies", "movie", moviesQuestions.size),',
            '            Genre("Science", "science", scienceQuestions.size),',
            '            Genre("History", "history_edu", historyQuestions.size),',
            '            Genre("Music", "music_note", musicQuestions.size),',
            '            Genre("Tech", "computer", techQuestions.size),',
            '            Genre("Entertainment", "theater_comedy", entertainmentQuestions.size),',
            '            Genre("Geography", "public", geographyQuestions.size),',
            '            Genre("Art", "palette", artQuestions.size),',
            '            Genre("Literature", "menu_book", literatureQuestions.size),',
            '            Genre("General Knowledge", "school", generalKnowledgeQuestions.size),',
            '            Genre("Pop Culture", "trending_up", popCultureQuestions.size),',
            "        )",
            "    }",
            "}",
            "",
        ]
    )

    return "\n".join(lines)


def main() -> None:
    specs = category_specs()
    ordered_genres, specs_by_genre, questions_by_spec = build_genre_index(specs)
    BANKS_OUTPUT.write_text(render_question_banks(ordered_genres, specs_by_genre, questions_by_spec), encoding="utf-8")
    OUTPUT.write_text(render_repository(ordered_genres, specs_by_genre), encoding="utf-8")
    print(f"Wrote {OUTPUT}")
    print(f"Wrote {BANKS_OUTPUT}")
    print(f"Categories: {len(specs)}")
    print(f"Total questions: {len(specs) * 210}")


if __name__ == "__main__":
    main()
