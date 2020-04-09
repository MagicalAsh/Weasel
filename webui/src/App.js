import React, {Component} from 'react';
import logo from './logo.svg';
import './App.css';

class App extends Component {
  render() {
    return (
      <div className="App">
        <HeaderView />
        <ResultView />
      </div>
    );
  }
}

class ResultView extends Component {
    constructor() {
        super();
        this.state = {results: []};

        ResultView.instance = this;
    }


    render = () => {
        return (
            <div>
                {this.state.results.map(i => (
                    <Result key={randomKey()} repoName={i.file_data.file_location} hits={i.hit_contexts}/>
                ))}
            </div>
        );
    };
}

class HeaderView extends Component {
    search_type = <StructuralHeaderView />;

    handleChange = (e) => {
        if (event.target.value === "regex_search") {
            this.search_type = <RegexHeaderView />;
        } else if (event.target.value === "struct_search") {
            this.search_type = <StructuralHeaderView />;
        }

        this.setState({value: event.target.value});
    };

    render = () => {
        return (
            <div className="App-header">
                <img src={logo} className="App-logo" alt="logo" />
                <select className="selector search" onChange={this.handleChange}>
                    <option value={"struct_search"}>Structural Search</option>
                    <option value={"regex_search"}>Regular Expression Search</option>
                </select>

                <br />

                {this.search_type}
            </div>
        );
    }
}

class StructuralHeaderView extends Component {
    render = () => {
        return (
            <div>
                <StructuralQueryBuilder />
            </div>
        );
    }
}

class StructuralQueryBuilder extends Component {
    constructor() {
        super();
        this.state = {};

        StructuralQueryBuilder.instance = this;
    }

    onClick = (e) => {
        let split = (s) => (s || "").split(/, */);

        let request = {
            "extends": split(this.state["Extends class(es):"] || ""),
            "interfaces": split(this.state["Interfaces:"] || ""),
            "modifiers": split(this.state["Modifiers:"] || ""),
            "field_names": split(this.state["Field Name(s):"] || ""),
            "method_names": split(this.state["Method Name(s):"] || "")
        };

        fetch("/search/structural/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(request)
        })
            .then(r => r.json())
            .then(obj => {
                ResultView.instance.setState({results: obj.hits || []})
            });

        return false;
    };

    render = () => {
        return (
            <div>
                <InputWithPreText preText={"Extends class(es):"}/>
                <InputWithPreText preText={"Interfaces:"}/>
                <InputWithPreText preText={"Modifiers:"}/>
                <InputWithPreText preText={"Field Name(s):"}/>
                <InputWithPreText preText={"Method Name(s):"}/>
                <button className="structSearchButton search" onClick={this.onClick}>
                    <i className="search-icon fas fa-search"/>
                </button>
            </div>
        );
    }
}

class InputWithPreText extends Component {
    onChange = (e) => {
        StructuralQueryBuilder.instance.state[this.props.preText] = event.target.value;
    };

    render = () => {
        return (
            <div>
                <p className="structSearch preInputText">{this.props.preText}</p>
                <input className="structSearch structuralInput" onChange={this.onChange}/>
            </div>
        );
    }
}

class RegexHeaderView extends Component {
    onEnterDown = (e) => {
        if (e.key === "Enter") {
            this.getHits();
        }
    };

    onClick = (e) => {
        this.getHits();
    };

    getHits = () => {
        let myHeaders = new Headers();
        myHeaders.append('Content-Type', 'application/json');

        fetch("http://localhost:9099/search/regex/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                "regex": this.state.regex_body
            })
        })
            .then(r => r.json())
            .then(obj => {
                ResultView.instance.setState({results: obj.hits || []})
            })
    };

    handleChange = (event) => {
        this.setState({"regex_body": event.target.value});
    };

    render = () => {
        return (
            <div>
                <input className="search-bar search" onKeyDown={this.onEnterDown} onChange={this.handleChange}/>
                <button className="search-button search search-icon" onClick={this.onClick}>
                    <i className="search-icon fas fa-search"/>
                </button>
            </div>
        );
    };
}

function loadFull(props) {
    return () => {
        let myHeaders = new Headers();
        myHeaders.append('Content-Type', 'application/json');

        fetch("/search/file/request", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                "content_location": props.repoName
            })
        })
        .then(r => r.json())
        .then(obj => {
            // todo fix this hack and make full source loading better
            let fileMatch = {};
            let fileHit = {
                matches: [],
                line_start: 0,
                line_end: obj.file.line_count
            };
            let matches = [];
            for (let hit of props.hits) {
                matches.push.apply(matches, hit.matches)
            }

            fileHit.matches = matches;

            fileHit.lines = obj.file.file_contents;
            obj.file.file_contents = undefined;
            fileMatch.file_data = obj.file;
            fileMatch.hit_contexts = [fileHit];


            ResultView.instance.setState({results: [fileMatch] || []});
        })
    }
}

const Result = (
    function render(props) {
        // to set line no starting, set style={{"counterReset": "line " + n}} on pre
        return (
            <div key={randomKey()} className="result">
                <div className="result-title"><h3>{props.repoName}</h3></div>
                <hr />
                <div className="result-body">
                    {
                        props.hits.map((hitContext, ind) => (
                            <pre className="result-text" style={{"counterReset": "line " + hitContext.line_start}} key={ind}>
                            {
                                hitContext.lines.map((val, ind) =>
                                     (
                                         hitContext.matches.includes(ind + hitContext.line_start + 1)
                                                ? <span key={ind} className="highlight">{val}</span>
                                                : <span key={ind}>{val}</span>
                                     )
                                )
                            }
                            </pre>
                        ))
                    }
                </div>
                <div className="fullSourceDiv">
                    <button className="fullSourceButton" onClick={loadFull(props)}>Load full source</button>
                </div>
            </div>
        );
    }
);

function randomKey() {
    //http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
    return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
}

export default App;
