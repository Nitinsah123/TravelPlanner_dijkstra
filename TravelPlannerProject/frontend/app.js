// List of cities — must match your backend cities from cities.csv
const cities = [
    "Delhi",
    "Mumbai",
    "Bangalore",
    "Chennai",
    "Kolkata",
    "Hyderabad"
    // Add more as per your data
];

// Fill dropdowns with city options
function populateDropdown(id) {
    const select = document.getElementById(id);
    cities.forEach(city => {
        const option = document.createElement("option");
        option.value = city;
        option.textContent = city;
        select.appendChild(option);
    });
}

populateDropdown("source");
populateDropdown("destination");

document.getElementById("findPath").addEventListener("click", () => {
    const source = document.getElementById("source").value;
    const destination = document.getElementById("destination").value;
    const resultDiv = document.getElementById("result");

    if (source === destination) {
        resultDiv.textContent = "Source and destination cannot be the same.";
        return;
    }

    const url = `http://localhost:4567/shortest-path?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`;

    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error("Network response was not ok " + response.statusText);
            }
            return response.json();
        })
        .then(path => {
            if (Array.isArray(path) && path.length > 0) {
                resultDiv.textContent = "Shortest Path: " + path.join(" -> ");
            } else {
                resultDiv.textContent = "No path found.";
            }
        })
        .catch(error => {
            resultDiv.textContent = "Error: " + error.message;
        });
});
