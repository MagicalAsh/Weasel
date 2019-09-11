import React, {Component} from 'react';
import logo from './logo.svg';
import './App.css';

class App extends Component {
  render() {
    return (
      <div className="App">
        <HeaderView />
        <div className="under-header" />
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
        console.log(this);
        return (
            <div>
                {this.state.results.map(i => (
                    <Result repoName={i.content_location} sourceText={i.file_contents}/>
                ))}
            </div>
        );
    };
}

class HeaderView extends Component {
    onEnterDown = (e) => {
        if (e.key === "Enter") {
            this.getHits();
        }
    };

    onClick = (e) => {
        console.log(this);
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
            <div className="App-header">
                <img src={logo} className="App-logo" alt="logo" />
                <input className="search-bar search" onKeyDown={this.onEnterDown} onChange={this.handleChange}/>
                <button className="search-button search search-icon" onClick={this.onClick}>
                    <i className="search-icon fas fa-search"/>
                </button>
            </div>
        );
    };
}

const Result = (
    function render(props) {
        // to set line no starting, set style={{"counterReset": "line " + n}} on pre
        return (
            <div className="result">
                <div className="result-title"><h3>{props.repoName}</h3></div>
                <hr />
                <div className="result-body">
                    <pre className="result-text">
                        {props.sourceText.map(val => <span>{val}</span>)}
                    </pre>
                </div>
            </div>
        );
    }
);

export default App;
