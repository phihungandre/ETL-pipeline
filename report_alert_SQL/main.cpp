#include <iostream>
#include <pqxx/pqxx>
#include <chrono>
#include <fstream>
#include <nlohmann/json.hpp>
#include <dpp/dpp.h>

using json = nlohmann::json;
using Citizen = std::pair<std::string, double>;

std::map<std::string, std::string> loadConfig(const std::string& filename) {
    std::ifstream file(filename);
    std::map<std::string, std::string> configValues;

    if (file.is_open()) {
        std::string line;
        while (std::getline(file, line)) {
            auto delimiterPos = line.find('=');
            auto name = line.substr(0, delimiterPos);
            auto value = line.substr(delimiterPos + 1);
            configValues[name] = value;
        }
        file.close();
    }

    return configValues;
}

void send_to_webhook(const std::string& message, const dpp::embed& embed, const std::string& WEBHOOKURL) {
    dpp::cluster bot("");

    dpp::webhook wh(WEBHOOKURL);

    bot.execute_webhook(wh, dpp::message(message).add_embed(embed));
}


std::vector<Citizen> parse_citizens_json(const std::string& json_string) {
    std::vector<Citizen> citizens;

    json j = json::parse(json_string);
    for (auto & it : j) {
        std::string name = it["name"].get<std::string>();
        double score = it["harmonyscore"].get<double>();
        citizens.emplace_back(name, score);
    }

    return citizens;
}

std::string citizens_to_string(const std::vector<Citizen>& citizens) {
    std::string citizens_str;
    for (const auto& citizen : citizens) {
        citizens_str += citizen.first + " (" + std::to_string(citizen.second) + "), ";
    }

    // remove the trailing comma and space
    if (!citizens_str.empty()) {
        citizens_str = citizens_str.substr(0, citizens_str.size() - 2);
    }

    return citizens_str;
}


int main() {
    try {
        // Load environment variables
        auto envVariables = loadConfig("../resources/.env");

        std::string DBNAME = envVariables["DBNAME"];
        std::string DBHOST = envVariables["DBHOST"];
        std::string DBUSER = envVariables["DBUSER"];
        std::string DBUSERPASSWORD = envVariables["DBUSERPASSWORD"];

        std::string WEBHOOKURL = envVariables["WEBHOOKURL"];

        pqxx::connection c("dbname=" + DBNAME + " host=" + DBHOST + " user="+ DBUSER + " password=" +
        DBUSERPASSWORD);

        pqxx::nontransaction w(c);

        // Set up LISTEN command
        w.exec("LISTEN report_insert");
        w.commit();

    while (true) {
            if (c.await_notification()) {
                // make a new query to get the latest row data
                pqxx::work W(c);
                pqxx::result R = W.exec("SELECT current_location_latitude, current_location_longitude, surrounding_citizens_temp, timestamp FROM reports ORDER BY id DESC LIMIT 1 FOR UPDATE;");

                dpp::embed embeded;
                // iterate over the result and append the data to the message
                for (auto row: R) {
                    auto citizens = parse_citizens_json(row[2].as<std::string>());

                    dpp::embed embed = dpp::embed().
                            set_color(dpp::colors::sti_blue).
                            set_title("GO ARREST!").
                            set_description("Some peasants aren't so harmonious anymore... "
                                            "A patrol must be sent to :").
                            set_thumbnail("https://image.similarpng.com/very-thumbnail/2020/05/3d-policeman-emoji-icons-with-police-cap-PNG.png").
                            add_field(
                            "Location:",
                            "Latitude: " + row[0].as<std::string>() + "\n"
                                                                      "Longitude: " + row[1].as<std::string>()
                    ).
                            add_field(
                            "Inharmonious citizens:",
                            citizens_to_string(citizens)
                    ).
                            add_field(
                            "Caught at:",
                            row[3].as<std::string>()
                    ).
                            set_image("https://previews.123rf.com/images/dreamcreation01/dreamcreation011608/dreamcreation01160800023/61197851-cartoon-caract%C3%A8re-de-voiture-de-police.jpg").
                            set_timestamp(time(0));

                    embeded = embed;
                    std::cout << "New: " + citizens_to_string(citizens) << std::endl;
                }
                send_to_webhook("", embeded, WEBHOOKURL); // send the data to the webhook
            }
    }

    } catch (std::exception const &e) {
        std::cerr << "Error .env : " << e.what() << std::endl;
        return 1;
    }

    return 0;
}
