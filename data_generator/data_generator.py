import random
import json
from datetime import datetime, timedelta
import os
from faker import Faker

fake = Faker()
number_of_reports = 1000


def load_phrases(file_path: str) -> list[str]:
    phrases = []
    if os.path.exists(file_path):
        with open(file_path, "r") as file:
            for line in file:
                phrase = line.strip()
                if phrase:
                    phrases.append(phrase)
    return phrases


def generate_data() -> dict:
    data = {"reports": []}
    phrases_file = os.path.join("data_generator", "premade_sentences.txt")  # Fichier contenant les phrases pré-définies

    # Charger les phrases pré-définies à partir du fichier
    phrases = load_phrases(phrases_file)

    for i in range(number_of_reports):
        report = {}
        report["harmonywatcher_id"] = str(i + 1).zfill(4)
        
        # Génération d'une localisation aléatoire
        latitude = round(random.uniform(-90, 90), 4)
        longitude = round(random.uniform(-180, 180), 4)
        report["current_location"] = {"latitude": latitude, "longitude": longitude}
        
        # Génération de citoyens environnants aléatoires
        num_citizens = random.randint(1, 5)
        citizens = []
        for _ in range(num_citizens):
            citizen = {}
            citizen["name"] = fake.name()
            if i % 100 == 0:
                citizen["harmonyscore"] = round(random.uniform(0, 0.5), 2)
            else:
                citizen["harmonyscore"] = round(random.uniform(0.5, 1), 2)
            citizens.append(citizen)
        report["surrounding_citizens"] = citizens
        
        if i % 100 == 0:
            report["words_heard"] = "I hate this state!"
        else:
            # Utiliser une phrase pré-définie avec une très faible probabilité
            if random.random() < 0.05 and phrases:
                report["words_heard"] = random.choice(phrases)
            else:
                report["words_heard"] = fake.sentence()

        if i % 100 == 0:
            report["alert"] = True
        else:
            report["alert"] = False
        
        # Génération d'un horodatage (timestamp) avec un delta aléatoire pour les dernières 24 heures
        now = datetime.now()
        seconds_delta = random.randint(0, 59)
        minutes_delta = random.randint(0, 59)
        hours_delta = random.randint(0, 23)
        timestamp = now - timedelta(seconds=seconds_delta, minutes=minutes_delta, hours=hours_delta)
        report["timestamp"] = timestamp.strftime("%Y-%m-%dT%H:%M:%SZ")
        
        data["reports"].append(report)

    return data


if __name__ == "__main__":
    # Générer les données et les enregistrer dans un fichier JSON
    data = generate_data()
    reports_path = os.path.join("data_generator", "reports.json")
    with open(reports_path, "w") as file:
        json.dump(data, file, indent=2)

    print("Données générées et enregistrées dans le fichier reports.json.")