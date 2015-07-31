var usersList = [
  {
    'id': 67,
    'title' : 'Mr.',
    'firstName' : 'John',
    'lastName' : 'Ceena',
    'middleName' : 'MiddleName',
    'email' : 'syedmohammed+222@neevtech.com',
    'role' : 'ADMIN'
  }, {
    'title' : 'Mr.',
    'firstName' : 'James',
    'lastName' : 'Williams',
    'middleName' : 'MiddleName',
    'email' : 'email',
    'role' : {'key' : 'ACCT_SERVICES', 'value' : 'Account Service'}
  },
  {
    'hillromId': 'HR000028',
    'title': 'Mr.',
    'firstName': 'Peter',
    'middleName': 'Smith',
    'lastName': 'Parker',
    'gender': 'male',
    'langKey': 'en',
    'zipcode': '560009',
    'city': 'Bangalore',
    'dob': '08/06/1992',
    'role': 'PATIENT'
  }
];


var clinicsList = [
  {
    "id": 14,
    "name": "Hill Rom",
    "address": "Neev",
    "zipcode": 56004,
    "city": "Bangalore",
    "phoneNumber": 9740353872,
    "faxNumber": 9942354883,
    "hillromId": 123,
    "state": "AL",
    "parent": true,
    "npiNumber": null,
    "deleted": false,
    "childClinics": [
      {
        "id": 65
      },
      {
        "id": 66
      }
    ]
  },
  {
    "id": 14,
    "name": "Neev Rom",
    "address": "Neev",
    "zipcode": 56004,
    "city": "Bangalore",
    "phoneNumber": 9740353872,
    "faxNumber": 9942354883,
    "hillromId": 123,
    "state": "AL",
    "parent": false,

    "npiNumber": null,
    "deleted": false,
    "parentClinic": {"name": "Hill Rom", "id": 13}
  }];



var roleEnum = {
  ADMIN : 'ADMIN',
  PATIENT : 'PATIENT',
  HCP : 'HCP',
  ACCT_SERVICES : 'ACCT_SERVICES',
  ASSOCIATES : 'ASSOCIATES',
  HILLROM_ADMIN : 'HILLROM_ADMIN',
  CLINIC_ADMIN : 'CLINIC_ADMIN',
  ANONYMOUS : 'ANONYMOUS'
};

var doctorsList = [
    {
        "id": 55,
        "email": "rishabhjain+HCP1@neevtech.com",
        "firstName": "Manipal",
        "lastName": "Ayer",
        "zipcode": 560009,
        "address": "Old Airport Road",
        "city": "Bangalore",
        "credentials": "Manipal Hospitals",
        "faxNumber": null,
        "primaryPhone": null,
        "mobilePhone": null,
        "speciality": "Manipal Hospitals",
        "state": "Karnataka",
        "clinics": [
            {
                "name": "Manipal Hospitals-main",
                "id": "1"
            }
        ],
        "deleted": false
    },
    {
        "id": 56,
        "email": "rishabhjain+HCP2@neevtech.com",
        "firstName": "Manipal",
        "lastName": "Ayer",
        "zipcode": 560009,
        "address": "Old Airport Road",
        "city": "Bangalore",
        "credentials": "Manipal Hospitals",
        "faxNumber": null,
        "primaryPhone": null,
        "mobilePhone": null,
        "speciality": "Manipal Hospitals",
        "state": "Karnataka",
        "clinics": [
            {
                "name": "Manipal Hospitals-child",
                "id": "2"
            },
            {
                "name": "Manipal Hospitals-child2",
                "id": "3"
            }
        ],
        "deleted": false
    }
]
