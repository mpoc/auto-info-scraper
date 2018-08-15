autogidas.lt car advert scraper
===============================
This program scapes a car advertisement website https://autogidas.lt/ for car advertisements. It gathers all the data available about a car, or if a make is specified, about all car advertisements of that make. The data is presented in a MySQL database.

The MySQL table should be created with such command:
```CREATE TABLE IF NOT EXISTS data (
	id	INT	NOT NULL,
	url TEXT NOT NULL,
	make	TEXT	NOT NULL,
	model	TEXT	NOT NULL,
	year	INT NOT NULL,
	price	INT NOT NULL,
	extraTaxes	BOOLEAN	NOT NULL,
	mileage	INT,
	sold BOOLEAN,
	techCheckUpAvailable BOOLEAN,
	techValidMonths INT,
	engineSize DECIMAL(5,1),
	fault	TEXT,
	enginePower	INT,
	numOfAddons	INT,
	PRIMARY KEY  (id)
);```

This project is an unfinished project, done as an exercise to practise web scraping, MySQL and Git. Eventually I will tu,rn this into a machine learning project to predict a price of a hypothetical car.