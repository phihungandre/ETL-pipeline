CREATE TABLE reports (
                         id SERIAL PRIMARY KEY,
                         harmonywatcher_id VARCHAR(255) NOT NULL,
                         current_location_latitude DOUBLE PRECISION NOT NULL,
                         current_location_longitude DOUBLE PRECISION NOT NULL,
                         words_heard TEXT,
                         surrounding_citizens JSONB,
                         alert BOOLEAN NOT NULL,
                         timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);


-- Function and it's trigger to notify the server when a new report is inserted
CREATE OR REPLACE FUNCTION report_insert_notify() RETURNS trigger AS $$
BEGIN
  NOTIFY report_insert;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER report_insert_trigger AFTER INSERT ON reports
    FOR EACH ROW EXECUTE FUNCTION report_insert_notify();

-- Function and it's trigger to convert the surrounding_citizens_temp column to jsonb
ALTER TABLE reports
    ADD COLUMN surrounding_citizens_temp VARCHAR;

CREATE OR REPLACE FUNCTION convert_json() RETURNS TRIGGER AS $$
BEGIN
  NEW.surrounding_citizens := NEW.surrounding_citizens_temp::jsonb;
  NEW.surrounding_citizens_temp := NULL;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER convert_json_trigger
    AFTER INSERT ON reports
    FOR EACH ROW
    EXECUTE PROCEDURE convert_json();
